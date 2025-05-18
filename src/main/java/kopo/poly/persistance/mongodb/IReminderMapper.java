package kopo.poly.persistance.mongodb;

import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.ReminderDTO;

public interface IReminderMapper {
    int insertReminder(String colNm, ReminderDTO pDTO) throws Exception;
    int deleteReminder(String colNm, PrescriptionDTO pDTO) throws Exception;
}
