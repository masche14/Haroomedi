package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IUserInfoService;
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

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/user")
public class UserController {

    private final IUserInfoService userInfoService;

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
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "redirect:/user/index";
        }

        String userId= (String) session.getAttribute("userId");

        if (userId != null){
            model.addAttribute("userId", userId);
        } else {
            model.addAttribute("userId", "");
        }

        return "/user/signin";
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
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "redirect:/user/index";
        }
        return "/user/email_verification";
    }

    @PostMapping("/getEmailExists")
    public ResponseEntity<CommonResponse<UserInfoDTO>> getEmailExists(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {

        log.info("{}.getEmailExists Start", this.getClass().getSimpleName());

        log.info("pDTO : {}", pDTO);

        UserInfoDTO rDTO = userInfoService.getUserEmailExists(pDTO);

        log.info("rDTO : {}", rDTO);

        log.info("{}.getEmailExists End", this.getClass().getSimpleName());

        return ResponseEntity.ok(
          CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rDTO)
        );
    }

    @GetMapping("/signup_detail")
    public String showSignupPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "redirect:/user/index";
        }

//        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
//        if (emailResultDTO==null){
//            return "redirect:/user/index";
//        }

//        if (emailResultDTO.existsYn().equals("Y")) {
//            String alert = "해당 이메일로 가입된 계정이 이미 존재합니다.";
//            session.setAttribute("error", alert);
//        }

        return "/user/signup_detail";
    }

    @GetMapping("/reset_pwd")
    public String showResetPwdPage(HttpSession session, Model model) {
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "redirect:/user/index";
        }

//        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
//        if (emailResultDTO==null){
//            return "redirect:/user/index";
//        }

        return "/user/reset_pwd";
    }

    @GetMapping("/find_id")
    public String showFindIdPage(HttpSession session, Model model) {
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "redirect:/user/index";
        }

//        UserInfoDTO emailResultDTO = (UserInfoDTO) session.getAttribute("emailResultDTO");
//        if (emailResultDTO==null){
//            return "redirect:/user/index";
//        }
//
//        if (emailResultDTO.existsYn().equals("Y")) {
//            model.addAttribute("userName", emailResultDTO.userName());
//            model.addAttribute("userId", emailResultDTO.userId());
//            session.setAttribute("userEmail", emailResultDTO.userEmail());
//            session.setAttribute("userId", emailResultDTO.userId());
//        } else {
//            // 이메일이 일치하지 않는 경우 빈 문자열을 명시적으로 전달
//            model.addAttribute("userName", "");
//            model.addAttribute("userId", "");
//        }

        return "/user/find_id";
    }

    @GetMapping("/index")
    public String showHomePage() {
        return "/user/index"; // /WEB-INF/views/index.jsp
    }

    @GetMapping("/delOrUpdate")
    public String showDelOrUpdate(HttpSession session) {
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

//        if (SS_USER_ID == null){
//            return "redirect:/user/index";
//        }

        return "/user/delOrUpdate";
    }

    @GetMapping("/pwd_verification")
    public String showPwdVerificationPage(HttpSession session){
        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        if (SS_USER_ID != null){
            return "/user/pwd_verification";
        } else {
            return "redirect:/user/index";
        }
    }

//    @GetMapping("/delInfo")
//    public String showDelInfo(HttpSession session){
//        String pwdVerifyResult = (String) session.getAttribute("pwdVerifyResult");
//
//        if (pwdVerifyResult==null) {
//            return "redirect:/user/index";
//        }
//
//        session.removeAttribute("pwdVerifyResult");
//
//        return "/user/delInfo";
//    }

    @GetMapping("/myPage")
    public String showMyPage(HttpSession session, Model model) {
        String pwdVerifyResult = (String) session.getAttribute("pwdVerifyResult");

//        if (pwdVerifyResult==null) {
//            return "redirect:/user/index";
//        }

        return "/user/myPage";
    }



}
