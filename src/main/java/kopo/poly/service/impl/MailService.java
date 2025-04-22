package kopo.poly.service.impl;

import jakarta.mail.internet.MimeMessage;
import kopo.poly.dto.MailDTO;
import kopo.poly.service.IMailService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class MailService implements IMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromMail;

    @Override
    public int doSendMail(MailDTO pDTO) {

        log.info("{}.doSendMail Start", this.getClass().getSimpleName());

        int res = 1;

        log.info("pDTO: {}", pDTO.toString());

        if (pDTO == null) {
            pDTO = new MailDTO();
        }

        String toMail = CmmUtil.nvl(pDTO.getToMail());
        String title = CmmUtil.nvl(pDTO.getTitle());
        String contents = CmmUtil.nvl(pDTO.getContents());

        log.info("toMail : {} / title : {} / contents : {}", toMail, title, contents);

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper messageHelper = new MimeMessageHelper(message, "UTF-8");

        try {
            messageHelper.setTo(toMail);           // 받는 사람
            messageHelper.setFrom(fromMail);       // 보내는 사람
            messageHelper.setSubject(title);       // 메일 제목
            messageHelper.setText(contents);       // 메일 내용

            mailSender.send(message);              // 메일 발송

        } catch (Exception e) {                    // 모든 에러 다 잡기
            res = 0;                               // 메일 발송이 실패했기 때문에 0으로 변경
            log.info("[ERROR] doSendMail : {}", e);
        }

        log.info("{}.doSendMail end!", this.getClass().getName());

        return res;
    }
}
