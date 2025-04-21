package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.MailDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.service.impl.MailService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RequestMapping(value = "/mail")
@RequiredArgsConstructor
@Controller
public class MailController {

    private final MailService mailService;

    @GetMapping(value = "mailForm")
    public String mailForm() {
        log.info("{}.mailForm Start!", this.getClass().getName());
        return "mail/mailForm";
    }

    @ResponseBody
    @PostMapping(value = "/sendMail")
    public MsgDTO sendMail(HttpServletRequest request) {

        // 로그 찍기(추후 찍은 로그를 통해 이 함수에 접근했는지 파악하기 용이하다.)
        log.info("{}.sendMail Start!", this.getClass().getName());

        String msg; // 발송 결과 메시지

        // 웹 URL로부터 전달받는 값들
        String toMail = CmmUtil.nvl(request.getParameter("toMail"));   // 받는사람
        String title = CmmUtil.nvl(request.getParameter("title"));     // 제목
        String contents = CmmUtil.nvl(request.getParameter("contents")); // 내용

        // 로그 출력 (받는 사람, 제목, 내용)
        log.info("toMail : {} / title : {} / contents : {}", toMail, title, contents);

        MailDTO pDTO = MailDTO.builder().build();

// 웹에서 받은 값을 DTO에 넣기

        pDTO.builder().toMail(toMail).title(title).contents(contents).build();
// 메일 발송하기
        int res = mailService.doSendMail(pDTO);

        if (res == 1) {  // 메일 발송 성공
            msg = "메일 발송하였습니다.";
        } else {         // 메일 발송 실패
            msg = "메일 발송을 실패하였습니다.";
        }
        //
        log.info(msg);

// 결과 메시지 전달하기
        MsgDTO dto = MsgDTO.builder().build();
        dto.builder().msg(msg).build();

// 로그 찍기 (추후 찍은 로그를 통해 이 함수 호출이 끝났는지 파악하기 용이하다.)
        log.info("{}.sendMail End!", this.getClass().getName());

        return dto;

    }
}
