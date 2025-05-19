package kopo.poly.service;

import kopo.poly.dto.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.List;
import java.util.Map;

public interface IHealthService {

    TilkoDTO getCertificateResult(TilkoDTO pDTO) throws Exception;

    List<PrescriptionDTO> getTestResult(TilkoDTO certificate) throws Exception;

    String getAnalyzeResult(Map<String, Object> testResult) throws Exception;

    Boolean loginCheck(TilkoDTO pDTO) throws Exception;

    int synchronizePrescriptions(TilkoDTO certificateResult) throws Exception;

    List<PrescriptionDTO> getPrescriptionList(UserInfoDTO pDTO) throws Exception;

    PrescriptionDTO updatePrescriptionInfo(PrescriptionDTO pDTO) throws Exception;

    int insertReminder(ReminderDTO pDTO) throws Exception;

    int deleteReminder(PrescriptionDTO pDTO) throws Exception;

    int updateReminderMealTime(UserInfoDTO pDTO) throws Exception;

    int updatePrescriptionAndReminderUserId(UserInfoDTO pDTO) throws Exception;

    ReminderDTO getReminderByPrescriptionId(ReminderDTO pDTO) throws Exception;

}
