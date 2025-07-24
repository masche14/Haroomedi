package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
//import kopo.poly.dto.HRecordDTO;
//import kopo.poly.service.IHealthService;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.*;
import kopo.poly.service.IHealthService;
import kopo.poly.service.IReminderAppendIntakeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/health")
public class HealthController {
    private final IHealthService healthService;
    private final IReminderAppendIntakeLogService reminderAppendIntakeLogService;

    @GetMapping("/auth")
    public String authPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if(!SS_USER.getRole().equals("user")){
            return "redirect:/admin/index";
        }

        log.info("SS_USER: {}", SS_USER);
        return "health/auth"; // templates/health/auth.html 로 매핑됨
    }

    @PostMapping("/certificate")
    public String postCertificate(HttpServletRequest request, HttpSession session, @RequestBody TilkoDTO pDTO) throws Exception {

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        String userId = SS_USER.getUserId();

        log.info("pDTO: {}", pDTO.toString());

        String selectedImageSrc = pDTO.getSelectedImageSrc();
        String selectedImageAlt =  pDTO.getSelectedImageAlt();

        session.setAttribute("selectedImageSrc", selectedImageSrc);
        session.setAttribute("selectedImageAlt", selectedImageAlt);

        TilkoDTO certificateResult = healthService.getCertificateResult(pDTO);
        certificateResult.setUserId(userId);

        log.info("certificateResult: {}", certificateResult.toString());

        session.setAttribute("certificateResult", certificateResult);

        return "redirect:/health/auth";
    }

    @GetMapping("/result")
    public String getResult(HttpSession session, Model model){
        String selectedImageSrc = (String) session.getAttribute("selectedImageSrc");
        model.addAttribute("selectedImageSrc", selectedImageSrc);
        return "health/result";
    }

    @PostMapping("/resultProcess")
    public ResponseEntity<CommonResponse<MsgDTO>> resultProcess(HttpSession session) throws Exception {

        log.info("{}.resultProcess Start", this.getClass().getSimpleName());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        TilkoDTO certificateResult = (TilkoDTO) session.getAttribute("certificateResult");

        // 인증결과 확인
        Boolean result = healthService.loginCheck(certificateResult);

        log.info("result: {}", result);

        MsgDTO dto = new MsgDTO();

        int res;
        String msg = "";

        if(!result){
            res = -1;
            msg = "인증에 실패하였습니다. 다시 진행해주세요.";

            dto.setMsg(msg);
            dto.setResult(res);

            return ResponseEntity.ok(
                    CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
            );
        }

        long startTime = System.currentTimeMillis();

        // 인증 성공 후 건강검진 데이터 조회
        res = healthService.synchronizePrescriptions(certificateResult);

        if (res == 1){
            msg = "최신 처방 데이터를 추가하였습니다.";
        } else {
            msg = "이미 최신 상태입니다.";
        }

        dto.setMsg(msg);
        dto.setResult(res);

        long endTime = System.currentTimeMillis();
        log.info("걸린 시간 : {}", endTime - startTime);

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
        );
    }

    @GetMapping("/prescriptionList")
    public String prescriptionList(HttpSession session, Model model) throws Exception {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        List<PrescriptionDTO> prescriptionList = null;

        if (SS_USER != null){
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            prescriptionList = healthService.getPrescriptionList(SS_USER);
        }

        log.info("prescriptionList: {}", prescriptionList);

        model.addAttribute("prescriptionList", prescriptionList);

        return "health/prescriptionList";
    }

    @PostMapping("/removeReminder")
    public ResponseEntity<CommonResponse<List<PrescriptionDTO>>> removeReminder(HttpSession session, @RequestBody PrescriptionDTO pDTO) throws Exception {
        log.info("{}.removeReminder Start", this.getClass().getSimpleName());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        PrescriptionDTO updatedDTO = healthService.updatePrescriptionInfo(pDTO);
        List<PrescriptionDTO> prescriptionList = new LinkedList<>();

        if (updatedDTO != null) {
            prescriptionList = healthService.getPrescriptionList(SS_USER);
            int res = healthService.deleteReminder(pDTO);

            if(res > 0){
                log.info("reminder deleted");
            }
        }

        log.info("{},removeReminder End", this.getClass().getSimpleName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), prescriptionList)
        );
    }

    @PostMapping("/setReminder")
    public ResponseEntity<CommonResponse<List<PrescriptionDTO>>> setReminder(HttpSession session, @RequestBody PrescriptionDTO pDTO) throws Exception {
        log.info("{}.setReminder Start", this.getClass().getSimpleName());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        String userId = SS_USER.getUserId();

        log.info("pDTO: {}", pDTO.toString());

        PrescriptionDTO updatedDTO = healthService.updatePrescriptionInfo(pDTO);
        log.info("updatedDTO: {}", updatedDTO.toString());

        if (updatedDTO != null) {
            String prescriptionId = updatedDTO.getPrescriptionId();
            int prescriptionPeriod = updatedDTO.getPrescriptionPeriod();
            int dailyIntakeCnt = updatedDTO.getDailyIntakeCnt();
            int toIntakeCnt = prescriptionPeriod * dailyIntakeCnt;
            int dailyToIntakeCnt = updatedDTO.getDailyIntakeCnt();
            int intakeCnt = 0;
            int leftIntakeCnt = toIntakeCnt - intakeCnt;
            List<String> mealTime = SS_USER.getMealTime();

            List<Map<String, Object>> dailyLog = new ArrayList<>();
            List<String> intakeTimes = new ArrayList<>();

            // ✅ mealTime의 30분 뒤로 intakeTimes 계산
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            if (mealTime.size() > dailyToIntakeCnt && dailyToIntakeCnt > 1) {
                for (int i = 0; i < dailyToIntakeCnt; i++) {
                    int index = Math.round((float) i * (mealTime.size() - 1) / (dailyToIntakeCnt - 1));
                    String originalTime = mealTime.get(index);

                    Date parsedTime = sdf.parse(originalTime);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsedTime);
                    cal.add(Calendar.MINUTE, 30);

                    String updatedTime = sdf.format(cal.getTime());
                    intakeTimes.add(updatedTime);
                }
            } else {
                for (String originalTime : mealTime) {
                    Date parsedTime = sdf.parse(originalTime);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsedTime);
                    cal.add(Calendar.MINUTE, 30);

                    String updatedTime = sdf.format(cal.getTime());
                    intakeTimes.add(updatedTime);
                }
            }

            // ✅ intakeTime 생성
            for (String timeStr : intakeTimes) {
                Map<String, Object> map = new HashMap<>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String todayStr = dateFormat.format(new Date());

                String dateTimeStr = todayStr + " " + timeStr;
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date intakeTime = fullFormat.parse(dateTimeStr);

                map.put("intakeTime", intakeTime);
                map.put("intakeYn", "N");

                dailyLog.add(map);
            }

            ReminderDTO reminderDTO = new ReminderDTO();

            reminderDTO.setPrescriptionId(prescriptionId);
            reminderDTO.setUserId(userId);
            reminderDTO.setMealTime(mealTime);
            reminderDTO.setToIntakeCnt(toIntakeCnt);
            reminderDTO.setDailyToIntakeCnt(dailyToIntakeCnt);
            reminderDTO.setIntakeCnt(intakeCnt);
            reminderDTO.setLeftIntakeCnt(leftIntakeCnt);
            reminderDTO.setIntakeLog(dailyLog);

            log.info("reminderDTO: {}", reminderDTO.toString());

            int res = healthService.insertReminder(reminderDTO);

            if (res > 0) {
                log.info("reminder added");
            }
        }

        List<PrescriptionDTO> prescriptionList = healthService.getPrescriptionList(SS_USER);
        log.info("prescriptionList: {}", prescriptionList);

        log.info("{}.setReminder End", this.getClass().getSimpleName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), prescriptionList)
        );
    }

    @GetMapping("/reminder")
    public String getReminder(HttpSession session, @RequestParam("prescriptionId") String prescriptionId, Model model) throws Exception {
        log.info("{}.getReminder Start", this.getClass().getSimpleName());

        ReminderDTO pDTO = new ReminderDTO();

        pDTO.setPrescriptionId(prescriptionId);

        ReminderDTO rDTO = healthService.getReminderByPrescriptionId(pDTO);

        log.info("rDTO : {}", rDTO.toString());

        model.addAttribute("reminder", rDTO);

        return "health/reminder";
    }

    @PostMapping("/updateIntakeLog")
    public ResponseEntity<CommonResponse<ReminderDTO>> updateIntakeLog(HttpSession session, @RequestBody ReminderDTO pDTO) throws Exception {
        log.info("{}.updateIntakeLog Start", this.getClass().getSimpleName());

        log.info("pDTO: {}", pDTO.toString());

        int res = healthService.updateIntakeLog(pDTO);

        ReminderDTO rDTO = healthService.getReminderByPrescriptionId(pDTO);

        log.info("rDTO : {}", rDTO.toString());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rDTO));
    }

    @GetMapping("/selfAppend")
    public ResponseEntity<String> testSyncReminder() throws Exception {
        String result = reminderAppendIntakeLogService.appendIntakeLog();
        return ResponseEntity.ok("결과 : " + result);
    }
}
