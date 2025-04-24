package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
//import kopo.poly.dto.HRecordDTO;
//import kopo.poly.service.IHealthService;
import kopo.poly.dto.HRecordDTO;
import kopo.poly.service.IHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    public String authPage() {
        return "/health/auth"; // templates/health/auth.html 로 매핑됨
    }

    @PostMapping("/certificate")
    public String postCertificate(HttpServletRequest request, HttpSession session) throws Exception {

        String PrivateAuthType = request.getParameter("PrivateAuthType");
        log.info(PrivateAuthType);
        String UserName = request.getParameter("UserName");
        String BirthDate = request.getParameter("BirthDate");
        String UserCellphoneNumber = request.getParameter("UserCellphoneNumber");
        String selectedImageSrc = request.getParameter("selectedImageSrc");
        String selectedImageAlt =  request.getParameter("selectedImageAlt");

        log.info("PrivateAuthType: {}", PrivateAuthType);
        log.info("UserName: {}", UserName);
        log.info("BirthDate: {}", BirthDate);
        log.info("UserCellphoneNumber: {}", UserCellphoneNumber);
        log.info("selectedImageSrc: {}", selectedImageSrc);
        log.info("selectedImageAlt: {}", selectedImageAlt);

        session.setAttribute("selectedImageSrc", selectedImageSrc);
        session.setAttribute("selectedImageAlt", selectedImageAlt);

        Map<String, Object> certificateResult = healthService.getCertificateResult(PrivateAuthType, UserName, BirthDate, UserCellphoneNumber);
        session.setAttribute("certificateResult", certificateResult);

        return "redirect:/health/result";
    }

    @GetMapping("/result")
    public String getResult(HttpSession session, Model model){
        String selectedImageSrc = (String) session.getAttribute("selectedImageSrc");
        model.addAttribute("selectedImageSrc", selectedImageSrc);
        return "/health/result";
    }

    @PostMapping("/resultProcess")
    public String resultProcess(HttpSession session) throws Exception {

        String SS_USER_ID = (String) session.getAttribute("SS_USER_ID");

        Map<String, Object> certificateResult = (Map<String, Object>) session.getAttribute("certificateResult");

        // 인증결과 확인
        Boolean result = healthService.loginCheck(certificateResult);

        log.info("result: {}", result);

        if(!result){
            return "redirect:/health/certificateError";
        }

        HRecordDTO pDTO = new HRecordDTO();
        pDTO.setUserId(SS_USER_ID);

//        long startTime = System.currentTimeMillis();

//        // 인증 성공 후 건강검진 데이터 조회
//        List<Map<String, Object>> health = healthService.getTestResult(certificateResult, pDTO);
//
//        if (health.isEmpty()) {
//
//            log.info("이미 최신상태입니다.");
//
//            return "redirect:/health/analyzeResult";
//        }
//
//        log.info("건강검진 데이터 : {}",health.toString());
//
//        session.setAttribute("healthResult", health);
//
//        String analyzeResult = "";
//
//        for (Map<String, Object> healthMap : health) {
//
//            log.info("분석 검사결과 : {}",healthMap.toString());
//
//            analyzeResult = healthService.getAnalyzeResult(healthMap);
//            session.setAttribute("analyzeResult", analyzeResult);
//
//        }
//
//        long endTime = System.currentTimeMillis();
//        log.info("걸린 시간 : {}", endTime - startTime);

        return "redirect:/health/analyzeResult";
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
}
