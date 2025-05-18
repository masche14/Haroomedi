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

            if (mealTime.size() > dailyToIntakeCnt){
                String firstintakeTime = "";
                String lastintakeTime = "";
                if (dailyToIntakeCnt == 1){
                    firstintakeTime = mealTime.get(0);
                    intakeTimes.add(firstintakeTime);
                } else {
                    firstintakeTime = mealTime.get(0);
                    lastintakeTime = mealTime.get(mealTime.size() - 1);

                    intakeTimes.add(firstintakeTime);
                    intakeTimes.add(lastintakeTime);
                }
            } else {
                intakeTimes = mealTime;
            }

            for (String timeStr : intakeTimes){
                Map<String, Object> map = new HashMap<>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String todayStr = dateFormat.format(new Date()); // 예: "2025-05-18"

                // 오늘 날짜 + 시간 문자열 조합
                String dateTimeStr = todayStr + " " + timeStr; // 예: "2025-05-18 07:00"

                // 최종 Date 객체로 파싱
                SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                Date intakeTime = fullFormat.parse(dateTimeStr);

                String intakeYn = "N";

                map.put("intakeTime", intakeTime);
                map.put("intakeYn", intakeYn);

                dailyLog.add(map);

                map = null;
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
