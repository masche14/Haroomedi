package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.BanDTO;
import kopo.poly.dto.LoginDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IReminderMapper;
import kopo.poly.service.*;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.DateUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/user")
public class UserController {

    private final IUserInfoService userInfoService;
    private final IHealthService healthService;
    private final IChatService chatService;
    private final ILoginService loginService;
    private final IBanService banService;

    @PostMapping("/setReferrer")
    public String setReferrer(HttpSession session, HttpServletRequest request, Model model) {
        log.info("referrer : {}", request.getHeader("referer"));
        session.setAttribute("referrer", request.getHeader("referer"));
        return "redirect:/user/signin";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); // 세션 무효화
        return "redirect:/user/index";
    }

    @GetMapping("/signin")
    public String showSigninPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null) {
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            return "redirect:/user/index";
        }


        String referrer = (String) session.getAttribute("referrer");
        String ref = "";

        if (referrer != null) {
            ref = referrer.replace("http://localhost:11000", "");

        } else {
            ref = "/user/index";
        }

        log.info("ref : {}", ref);

        model.addAttribute("ref", ref);

        String userId= (String) session.getAttribute("userId");

        if (userId != null){
            model.addAttribute("userId", userId);
            session.removeAttribute("userId");
        } else {
            model.addAttribute("userId", "");
        }

        return "user/signin";
    }

    @PostMapping("/signin")
    public ResponseEntity<CommonResponse<MsgDTO>> signin(HttpSession session, @RequestBody UserInfoDTO pDTO, Model model) {

        log.info("{}.signin start", this.getClass().getSimpleName());

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg = "";

        log.info("password : {}", pDTO.getPassword());


        try {
            String encPassword = EncryptUtil.encHashSHA256(pDTO.getPassword());
            pDTO.setPassword(encPassword);

            log.info("encPassword : {}", encPassword);

            UserInfoDTO rDTO = userInfoService.getLogin(pDTO);

            if (!CmmUtil.nvl(rDTO.getUserId()).isEmpty()) {
                log.info("rDTO password : {}", rDTO.getPassword());

                if (CmmUtil.nvl(pDTO.getPassword()).equals(CmmUtil.nvl(rDTO.getPassword()))) {
                    session.removeAttribute("emailResultDTO");

                    res = 1;

                    msg = "로그인에 성공하였습니다.";

                    session.setAttribute("SS_USER", rDTO);

                    LoginDTO loginDTO = new LoginDTO();
                    loginDTO.setUserId(rDTO.getUserId());
                    loginDTO.setUserName(rDTO.getUserName());
                    loginDTO.setLoginAt(new Date());

                    log.info("rDTO role : {}", rDTO.getRole());

                    loginDTO.setRole(rDTO.getRole());

                    int success = loginService.insertLoginInfo(loginDTO);

                    if (success > 0) {
                        log.info("로그인 기록 저장 완료");
                    } else {
                        log.info("로그인 기록 저장 실패");
                    }

                } else {
                    msg = "비밀번호가 올바르지 않습니다.";
                }

            } else {
                msg = "존재하지 않는 회원 아이디 입니다.";
            }
        } catch (Exception e) {
            msg = "시스템 문제로 로그인이 실패하였습니다.";
            res = 2;
            log.info(e.toString());
        } finally {
            log.info("{}.loginProc End", this.getClass().getName());
        }

        dto.setResult(res);
        dto.setMsg(msg);

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
        );
    }

    @PostMapping("/setSource")
    public String setSource(HttpSession session, HttpServletRequest request, Model model) {

        String source = request.getParameter("source");

        log.info("source : {}", source);

        session.setAttribute("source", source);

        return "redirect:/user/email_verification";
    }

    @GetMapping("/email_verification")
    public String showEmailVerificationPage(HttpSession session) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            return "redirect:/user/index";
        }
        return "user/email_verification";
    }

    @PostMapping("/getEmailExists")
    public ResponseEntity<CommonResponse<UserInfoDTO>> getEmailExists(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {

        log.info("{}.getEmailExists Start", this.getClass().getSimpleName());

        log.info("pDTO : {}", pDTO.toString());

        String encEmail = EncryptUtil.encAES128CBC(CmmUtil.nvl(pDTO.getValue()));

        pDTO.setValue(encEmail);

        log.info("after encoding pDTO : {}", pDTO);

        BanDTO banDTO = banService.isBaned(pDTO);

        if (banDTO.getExistYn()!=null) {
            pDTO.setBanYn("Y");
            pDTO.setBanReason(banDTO.getReason());

            return ResponseEntity.ok(
                    CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), pDTO)
            );
        }

        UserInfoDTO rDTO = userInfoService.getUserEmailExists(pDTO);

        log.info("rDTO : {}", rDTO.toString());

        session.setAttribute("emailResultDTO", rDTO);

        log.info("{}.getEmailExists End", this.getClass().getSimpleName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rDTO)
        );
    }

    @PostMapping("/approveCode")
    public ResponseEntity<CommonResponse<MsgDTO>> approveCode(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.approveCode Start", this.getClass().getSimpleName());
        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");

        log.info("input_auth : {}", pDTO.getAuthNumber());
        log.info("auth : {}", emailResultDTO.getAuthNumber());

        MsgDTO dto = new MsgDTO();
        int res;
        String msg="";
        if (pDTO.getAuthNumber()==emailResultDTO.getAuthNumber()){
            msg = "인증에 성공하였습니다.";
            if (emailResultDTO.getExistYn().equals("Y")){
                res = 2;
            } else {
                res = 1;
            }
        }else {
            res = 0;
            msg = "인증에 실패하였습니다. 인증코드를 다시 확인해주세요.";
        }
        dto.setResult(res);
        dto.setMsg(msg);

        log.info("{}.approveCode End", this.getClass().getSimpleName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @PostMapping("email_verification")
    public String emailVerification(HttpSession session) throws Exception {
        String source = (String) session.getAttribute("source");
        log.info("source : {}", source);
        return "redirect:/user/"+source;
    }

    @GetMapping("/signup_detail")
    public String showSignupPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            return "redirect:/user/index";
        }

        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
        if (emailResultDTO==null){
            return "redirect:/user/index";
        }

        log.info("ExistYN : {}", emailResultDTO.getExistYn());

        if (emailResultDTO.getExistYn().equals("Y")) {
            log.info("해당 이메일로 가입된 계정이 이미 존재합니다.");
            String alert = "해당 이메일로 가입된 계정이 이미 존재합니다.";
            model.addAttribute("error", alert);
        }

        return "user/signup_detail";
    }

    @PostMapping("/signup_detail")
    public ResponseEntity<CommonResponse<MsgDTO>> signupDetail(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {

        log.info("{}.signupDetail Start", this.getClass().getSimpleName());

        MsgDTO dto = new MsgDTO();
        int res=0;
        String msg="";

        log.info("pDTO : {}", pDTO.toString());

        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
        String userEmail = emailResultDTO.getUserEmail();
        log.info("userEmail : {}", userEmail);
        pDTO.setUserEmail(userEmail);

        String encPhoneNumber = EncryptUtil.encAES128CBC(pDTO.getPhoneNumber());
        pDTO.setPhoneNumber(encPhoneNumber);

        log.info("phoneNumber : {}", EncryptUtil.decAES128CBC(CmmUtil.nvl(pDTO.getPhoneNumber())));

        String encPassword = EncryptUtil.encHashSHA256(CmmUtil.nvl(pDTO.getPassword()));
        pDTO.setPassword(encPassword);
        log.info("After Encoding pDTO : {}", pDTO);

        pDTO.setFieldName("phoneNumber");
        pDTO.setValue(pDTO.getPhoneNumber());

        BanDTO banDTO = banService.isBaned(pDTO);

        if (banDTO.getExistYn()!=null) {
            pDTO.setBanYn("Y");
            pDTO.setBanReason(banDTO.getReason());

            res = -1;
            msg = "차단된 유저의 핸드폰 번호입니다.\n차단 사유 : "+pDTO.getBanReason();

            dto.setMsg(msg);
            dto.setResult(res);

            return ResponseEntity.ok(
                    CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
            );
        }

        pDTO.setRegId(pDTO.getUserId());
        pDTO.setRegDt(DateUtil.getDateTime("yyyyMMddHHmmss"));
        pDTO.setRole("user");

        res = userInfoService.insertUserInfo(pDTO);

        if (res > 0) {
            msg = "성공적으로 회원가입이 완료되었습니다.";
            session.setAttribute("userId", pDTO.getUserId());
        } else {
            msg = "회원가입에 실패하였습니다.";
        }

        dto.setResult(res);
        dto.setMsg(msg);

        log.info("{}.signupDetail End", this.getClass().getSimpleName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @PostMapping("/checkDuplicate")
    public ResponseEntity<CommonResponse<MsgDTO>> checkDuplicate(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.checkDuplicate Start", this.getClass().getSimpleName());

        log.info("fieldName : {}", pDTO.getFieldName());
        log.info("value : {}", pDTO.getValue());

        UserInfoDTO rDTO = userInfoService.checkDuplicate(pDTO);

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg="";

        if (pDTO.getFieldName().equals("userId")){
            if (rDTO.getExistYn().equals("Y")){
                res = 1;
                msg = "이미 존재하는 아이디입니다.";
            }else{
                msg = "사용 가능한 아이디입니다.";
            }
        }else if (pDTO.getFieldName().equals("userNickname")){
            if (rDTO.getExistYn().equals("Y")){
                res = 1;
                msg = "이미 존재하는 닉네임입니다.";
            }else {
                msg = "사용 가능한 닉네임입니다.";
            }
        }

        dto.setResult(res);
        dto.setMsg(msg);

        log.info("{}.checkDuplicate End", this.getClass().getSimpleName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @GetMapping("/reset_pwd")
    public String showResetPwdPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            return "redirect:/user/index";
        }

        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
        if (emailResultDTO==null){
            return "redirect:/user/index";
        }

        model.addAttribute("existYN", emailResultDTO.getExistYn());
        model.addAttribute("userId", emailResultDTO.getUserId());

        return "user/reset_pwd";
    }

    @PostMapping("/reset_pwd")
    public ResponseEntity<CommonResponse<MsgDTO>> resetPwd(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.resetPwd Start", this.getClass().getSimpleName());
        log.info("pDTO : {}", pDTO.toString());

        log.info("password : {}", pDTO.getPassword());

        String encPassword = EncryptUtil.encHashSHA256(pDTO.getPassword());
        pDTO.setPassword(encPassword);

        pDTO.setChgId(pDTO.getOrgId());
        pDTO.setChgDt(DateUtil.getDateTime("yyyyMMddHHmmss"));

        log.info("encPassword : {}", encPassword);

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg="";

        res = userInfoService.updateUserInfo(pDTO);
        log.info("res : {}", res);

        if (res == 1) {
            msg = "비밀번호가 변경되었습니다.";
        } else {
            msg = "비밀번호 변경에 실패하였습니다.";
        }

        dto.setResult(res);
        dto.setMsg(msg);

        log.info("{}.resetPwd End", this.getClass().getSimpleName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @GetMapping("/find_id")
    public String showFindIdPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            if(!SS_USER.getRole().equals("user")){
                return "redirect:/admin/index";
            }
            return "redirect:/user/index";
        }

        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
        if (emailResultDTO==null){
            return "redirect:/user/index";
        }

        if (emailResultDTO.getExistYn().equals("Y")) {
            log.info("userName : {}", emailResultDTO.getUserName());
            log.info("userId : {}", emailResultDTO.getUserId());

            model.addAttribute("userName", emailResultDTO.getUserName());
            model.addAttribute("userId", emailResultDTO.getUserId());
            session.setAttribute("userEmail", emailResultDTO.getUserEmail());
            session.setAttribute("userId", emailResultDTO.getUserId());
        } else {
            // 이메일이 일치하지 않는 경우 빈 문자열을 명시적으로 전달
            model.addAttribute("userName", "");
            model.addAttribute("userId", "");
        }

        return "user/find_id";
    }

    @GetMapping("/index")
    public String showHomePage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            log.info("SS_USER : {}", SS_USER.toString());
        }

        return "user/index";
    }

    @GetMapping("/pwd_verification")
    public String showPwdVerificationPage(HttpSession session){
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null){
            return "user/pwd_verification";
        } else {
            return "redirect:/user/index";
        }
    }

    @PostMapping("/pwd_verification")
    public ResponseEntity<CommonResponse<MsgDTO>> verifyPwdVerification(HttpSession session, @RequestBody UserInfoDTO pDTO) {
        log.info("{}.verifyPwdVerification Start", this.getClass().getSimpleName());
        log.info("password : {}", pDTO.getPassword());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        String userId = SS_USER.getUserId();
        pDTO.setUserId(userId);

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg="";

        try {
            String encPassword = EncryptUtil.encHashSHA256(pDTO.getPassword());
            pDTO.setPassword(encPassword);

            log.info("encPassword : {}", encPassword);

            UserInfoDTO rDTO = userInfoService.getLogin(pDTO);

            if (!CmmUtil.nvl(rDTO.getUserId()).isEmpty()) {
                log.info("rDTO password : {}", rDTO.getPassword());

                if (CmmUtil.nvl(pDTO.getPassword()).equals(CmmUtil.nvl(rDTO.getPassword()))) {
                    res = 1;
                    msg = "비밀번호 인증에 성공하였습니다.";

                    String pwdVerifyResult = "Y";
                    session.setAttribute("pwdVerifyResult", pwdVerifyResult);
                } else {
                    msg = "비밀번호가 올바르지 않습니다.";
                }

            } else {
                msg = "존재하지 않는 회원 아이디 입니다.";
            }
        } catch (Exception e) {
            msg = "시스템 문제로 로그인이 실패하였습니다.";
            res = 2;
            log.info(e.toString());
        } finally {
            log.info("{}.verifyPwdVerification End", this.getClass().getName());
        }

        dto.setResult(res);
        dto.setMsg(msg);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @GetMapping("/delOrUpdate")
    public String showDelOrUpdate(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        String pwdVerifyResult = (String) session.getAttribute("pwdVerifyResult");
        model.addAttribute("pwdVerifyResult", pwdVerifyResult);
        session.removeAttribute("pwdVerifyResult");

        if (SS_USER == null){
            return "redirect:/user/index";
        }

        return "user/delOrUpdate";
    }

    @PostMapping("/delInfo")
    public ResponseEntity<CommonResponse<MsgDTO>> delInfo(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.delInfo Start", this.getClass().getSimpleName());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        pDTO.setUserId(SS_USER.getUserId());

        log.info("userId : {}", pDTO.getUserId());

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg="";

        res = userInfoService.deleteUserInfo(pDTO);

        log.info("res : {}", res);

        if (res>0){
            msg="회원탈퇴가 완료되었습니다.";

            int success = healthService.deleteAllReminder(pDTO);

            if (success > 0){
                log.info("User's Reminder Deleted!");
            }

            success = healthService.deleteAllPrescription(pDTO);

            if (success > 0){
                log.info("User's Prescription Deleted!");
            }

            success = chatService.deleteAllChat(pDTO);

            if (success > 0){
                log.info("User's Chat Deleted!");
            }

        } else {
            msg="회원탈퇴에 실패하였습니다.";
        }

        dto.setResult(res);
        dto.setMsg(msg);

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }

    @GetMapping("/myPage")
    public String showMyPage(HttpSession session, Model model) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        if (SS_USER == null){
            return "redirect:/user/index";
        }
        return "user/myPage";
    }

    @PostMapping("/updateInfo")
    public ResponseEntity<CommonResponse<MsgDTO>> myPage(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.updateInfo Start", this.getClass().getSimpleName());
        log.info("pDTO : {}", pDTO.toString());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        String userId = SS_USER.getUserId();
        pDTO.setOrgId(userId);

        log.info("password : {}", pDTO.getPassword());

        if (!CmmUtil.nvl(pDTO.getPassword()).isBlank()){
            String encPassword = EncryptUtil.encHashSHA256(pDTO.getPassword());
            pDTO.setPassword(encPassword);

            log.info("encPassword : {}", encPassword);
        }

        if (!CmmUtil.nvl(pDTO.getUserEmail()).isBlank()){
            String encUserEmail = EncryptUtil.encAES128CBC(pDTO.getUserEmail());
            pDTO.setUserEmail(encUserEmail);

            log.info("encUserEmail : {}", encUserEmail);
        }

        if (!CmmUtil.nvl(pDTO.getPhoneNumber()).isBlank()){
            String encPhoneNumber = EncryptUtil.encAES128CBC(pDTO.getPhoneNumber());
            pDTO.setPhoneNumber(encPhoneNumber);
        }

        pDTO.setChgId(pDTO.getOrgId());
        pDTO.setChgDt(DateUtil.getDateTime("yyyyMMddHHmmss"));

        MsgDTO dto = new MsgDTO();
        int res = 0;
        String msg="";

        res = userInfoService.updateUserInfo(pDTO);

        log.info("res : {}", res);

        if (res == 1){
            msg = "회원정보 수정 완료";

            if (!pDTO.getMealTime().isEmpty()) {
                int success = healthService.updateReminderMealTime(pDTO);
                if (success == 1){
                    log.info("Reminder's MealTime updated");
                }
            }

            if (!CmmUtil.nvl(pDTO.getUserId()).isBlank()){
                int success = healthService.updatePrescriptionAndReminderUserId(pDTO);
                if (success == 1){
                    log.info("Prescription And Reminder's userId updated");
                }
            }
        }


        if (CmmUtil.nvl(pDTO.getUserId()).isBlank()){
            pDTO.setUserId(pDTO.getOrgId());
        }

        UserInfoDTO rDTO = userInfoService.getLogin(pDTO);

        log.info("rDTO : {}", rDTO.toString());

        session.setAttribute("SS_USER", rDTO);

        dto.setResult(res);
        dto.setMsg(msg);

        log.info("{}.updateInfo End", this.getClass().getSimpleName());

        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto));
    }



}