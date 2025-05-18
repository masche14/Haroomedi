package kopo.poly.service.impl;

import kopo.poly.dto.ReminderDTO;
import kopo.poly.persistance.mongodb.IReminderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderAppendIntakeLogService {

    private final IReminderMapper reminderMapper;

    private final String colNm = "Reminder";

    @Scheduled(cron = "0 0 0 * * *") // 매일 00시에 실행
    public void appendDailyIntakeLog() throws Exception {

        log.info("===== 📆 매일 intakeLog 추가 스케줄 시작 =====");

        // 1. leftIntakeCnt > 0 인 reminder 조회
        List<ReminderDTO> rList = reminderMapper.getReminderListWithLeftIntake(colNm);

        for (ReminderDTO rDTO : rList) {
            String prescriptionId = rDTO.getPrescriptionId();
            List<String> mealTime = rDTO.getMealTime(); // ["07:30", "12:30", "19:30"]
            int dailyToIntakeCnt = rDTO.getDailyToIntakeCnt();


            List<Map<String, Object>> dailyLog = new ArrayList<>();
            List<String> intakeTimes = new ArrayList<>();

            if (mealTime.size() > dailyToIntakeCnt) {
                for (int i = 0; i < dailyToIntakeCnt; i++) {
                    // 비례 인덱스 계산: 0, 중간, 마지막 등 간격 유지
                    int index = Math.round((float) i * (mealTime.size() - 1) / (dailyToIntakeCnt - 1));
                    intakeTimes.add(mealTime.get(index));
                }
            } else {
                intakeTimes = mealTime;
            }

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

            // DTO에 세팅
            rDTO.setIntakeLog(dailyLog);

            // 2. 해당 prescriptionId 문서의 intakeLog에 push
            int res = reminderMapper.appendIntakeLog(colNm, rDTO);

            if (res > 0) {
                log.info("복약일정 생성 성공");
            }
        }

        log.info("===== ✅ 매일 intakeLog 추가 스케줄 완료 =====");
    }
}
