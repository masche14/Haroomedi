package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/")
public class MainController {

    @GetMapping("")
    public String index(HttpSession session) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");
        if (SS_USER != null) {
            log.info("SS_USER: {}", SS_USER);
        }
        return "user/index";
    }
}
