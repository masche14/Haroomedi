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
    public void appendSchedule() throws Exception {
        String result = this.appendIntakeLog();
        log.info("결과 : {}",result);
    }

    public String appendIntakeLog() throws Exception {

        String result = "";

        log.info("===== 📆 매일 intakeLog 추가 스케줄 시작 =====");

        // 1. leftIntakeCnt > 0 인 reminder 조회
        List<ReminderDTO> rList = reminderMapper.getReminderListWithLeftIntake(colNm);

        for (ReminderDTO rDTO : rList) {
            String prescriptionId = rDTO.getPrescriptionId();
            List<String> mealTime = rDTO.getMealTime(); // ["07:30", "12:30", "19:30"]
            int dailyToIntakeCnt = rDTO.getDailyToIntakeCnt();

            log.info("prescriptionId: {}", prescriptionId);
            log.info("mealTime: {}", mealTime);
            log.info("dailyToIntakeCnt: {}", dailyToIntakeCnt);

            List<Map<String, Object>> dailyLog = new ArrayList<>();
            List<String> intakeTimes = new ArrayList<>();

            // ✅ 복용 간격 계산 (mealTime에서 30분 뒤 시간 추출)
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

            if (mealTime.size() > dailyToIntakeCnt && dailyToIntakeCnt > 1) {
                for (int i = 0; i < dailyToIntakeCnt; i++) {
                    int index = Math.round((float) i * (mealTime.size() - 1) / (dailyToIntakeCnt - 1));
                    String originalTime = mealTime.get(index);

                    Date parsedTime = sdf.parse(originalTime);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsedTime);
                    cal.add(Calendar.MINUTE, 30); // 30분 추가

                    String updatedTime = sdf.format(cal.getTime());
                    intakeTimes.add(updatedTime);
                }
            } else {
                for (String originalTime : mealTime) {
                    Date parsedTime = sdf.parse(originalTime);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsedTime);
                    cal.add(Calendar.MINUTE, 30); // 30분 추가

                    String updatedTime = sdf.format(cal.getTime());
                    intakeTimes.add(updatedTime);
                }
            }

            log.info("intakeTimes (30분 뒤): {}", intakeTimes.toString());

            // ✅ 기존 intakeLog에서 이미 추가된 시간 수집
            Set<Date> existingTimes = new HashSet<>();
            if (rDTO.getIntakeLog() != null) {
                for (Map<String, Object> log : rDTO.getIntakeLog()) {
                    existingTimes.add((Date) log.get("intakeTime"));
                }
            }

            // ✅ 오늘 기준으로 intakeTime 생성
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String todayStr = dateFormat.format(new Date());
            SimpleDateFormat fullFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            for (String timeStr : intakeTimes) {
                String dateTimeStr = todayStr + " " + timeStr;
                Date intakeTime = fullFormat.parse(dateTimeStr);

                // ✅ 중복이면 추가하지 않음
                if (existingTimes.contains(intakeTime)) {
                    log.info("중복된 시간 {} 은 추가하지 않음", intakeTime);
                    continue;
                }

                Map<String, Object> map = new HashMap<>();
                map.put("intakeTime", intakeTime);
                map.put("intakeYn", "N");

                dailyLog.add(map);
            }

            log.info("추가할 dailyLog: {}", dailyLog);

            // ✅ 새로 추가할 게 있을 경우에만 DB push
            if (!dailyLog.isEmpty()) {
                rDTO.setIntakeLog(dailyLog);

                int res = reminderMapper.appendIntakeLog(colNm, rDTO);

                if (res > 0) {
                    result = "📌 복약일정 추가 성공";
                }
            } else {
                result = "📌 중복된 시간만 존재하여 추가 일정 없음";
            }
        }

        log.info("===== ✅ 매일 intakeLog 추가 스케줄 종료 =====");

        return result;
    }


}
