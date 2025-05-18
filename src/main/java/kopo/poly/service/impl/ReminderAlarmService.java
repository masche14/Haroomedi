package kopo.poly.service.impl;

import kopo.poly.dto.MailDTO;
import kopo.poly.dto.ReminderDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IReminderMapper;
import kopo.poly.persistance.mongodb.IUserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderAlarmService {
    private final IReminderMapper reminderMapper;
    private final IUserInfoMapper userInfoMapper;
    private final MailService mailService;

    private final String colNm = "Reminder";
    private final String userColNm = "UserInfo";

    @Scheduled(cron = "0 * * * * *") // 매분 0초마다 실행
    public void sendReminderAlarm() throws Exception {

        log.info("===== 복약 알림 스케줄 시작 =====");

        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();

        cal.add(Calendar.MINUTE, 1);
        Date end = cal.getTime();

        List<ReminderDTO> reminderList = reminderMapper.getReminderListToSend(colNm, start, end);

        for (ReminderDTO rDTO : reminderList) {
            try {
                String userId = rDTO.getUserId();
                UserInfoDTO uDTO = new UserInfoDTO();
                uDTO.setUserId(userId);

                uDTO = userInfoMapper.getUserInfoByUserId(userColNm, uDTO);

                if (uDTO == null || uDTO.getUserEmail() == null) {
                    log.warn("이메일 정보 없음 - userId: {}", userId);
                    continue;
                }

                MailDTO dto = new MailDTO();
                dto.setTitle("복약알림 메일 전송");
                dto.setContents(String.format("복약 시간입니다. (%s)", new SimpleDateFormat("HH:mm").format(start)));
                dto.setToMail(uDTO.getUserEmail());

                log.info("메일 DTO: {}", dto);
                mailService.doSendMail(dto);
                log.info("메일 전송 완료: {}", dto.getToMail());

            } catch (Exception e) {
                log.error("알림 전송 실패 - userId: {}, error: {}", rDTO.getUserId(), e.getMessage());
            }
        }

        log.info("===== 복약 알림 스케줄 종료 =====");
    }
}
