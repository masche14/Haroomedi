package kopo.poly.service;

import kopo.poly.dto.HRecordDTO;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.List;
import java.util.Map;

public interface IHealthService {
    String getPublicKey() throws Exception;
    String encryptAES(SecretKey aesKey, IvParameterSpec iv, String plainText) throws Exception;
    String encryptRSA(String publicKeyStr, byte[] aesKey) throws Exception;
    Map<String, Object> getCertificateResult(String PrivateAuthType, String UserName, String BirthDate, String UserCellphoneNumber) throws Exception;
    List<Map<String, Object>> getTestResult(Map<String, Object> certificate, HRecordDTO pDTO) throws Exception;
    String getAnalyzeResult(Map<String, Object> testResult) throws Exception;
    Boolean loginCheck(Map<String, Object> certificate) throws Exception;
}
