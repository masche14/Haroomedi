package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
//import kopo.poly.dto.HRecordDTO;
//import kopo.poly.service.IHealthService;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.*;
import kopo.poly.service.IHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/health")
public class HealthController {
    private final IHealthService healthService;

    @GetMapping("/auth")
    public String authPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        log.info("SS_USER: {}", SS_USER);
        return "/health/auth"; // templates/health/auth.html 로 매핑됨
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
        return "/health/result";
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

    @GetMapping("/certificateError")
    public String getCertificateError(){
        return "/health/certificateError";
    }

    @GetMapping("/analyzeResult")
    public String analyzeResult(HttpSession session) throws Exception {
        List<Map<String, Object>> healthResult = (List<Map<String, Object>>) session.getAttribute("healthResult");
        String analyzeResult = (String) session.getAttribute("analyzeResult");

        if (healthResult != null && analyzeResult != null) {
            return "/health/analyzeResult";
        }else {
            return "/health/auth";
        }
    }

    @GetMapping("/prescriptionList")
    public String prescriptionList(HttpSession session, Model model) throws Exception {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        List<PrescriptionDTO> prescriptionList = null;

        if (SS_USER != null){
            prescriptionList = healthService.getPrescriptionList(SS_USER);
        }

        log.info("prescriptionList: {}", prescriptionList);

        model.addAttribute("prescriptionList", prescriptionList);

        return "/health/prescriptionList";
    }
}
