package kopo.poly.persistance.mongodb;

import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.ReminderDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.Date;
import java.util.List;

public interface IReminderMapper {
    int insertReminder(String colNm, ReminderDTO pDTO) throws Exception;
    int deleteReminder(String colNm, PrescriptionDTO pDTO) throws Exception;
    List<ReminderDTO> getReminderListToSend(String colNm, Date start, Date end) throws Exception;
    List<ReminderDTO> getReminderListWithLeftIntake(String colNm) throws Exception;
    int appendIntakeLog(String colNm, ReminderDTO pDTO) throws Exception;
    int updateMealTime(String colNm, UserInfoDTO pDTO) throws Exception;
    int updateUserId(String colNm, UserInfoDTO pDTO) throws Exception;
}
