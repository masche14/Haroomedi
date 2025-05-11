package kopo.poly.service;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.TilkoDTO;
import kopo.poly.dto.UserInfoDTO;

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
}
