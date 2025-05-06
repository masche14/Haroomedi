package kopo.poly.service;

import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.TilkoDTO;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.util.List;
import java.util.Map;

public interface IHealthService {
    String getPublicKey() throws Exception;
    String encryptAES(SecretKey aesKey, IvParameterSpec iv, String plainText) throws Exception;
    String encryptRSA(String publicKeyStr, byte[] aesKey) throws Exception;
    TilkoDTO getCertificateResult(TilkoDTO pDTO) throws Exception;
    List<Map<String, Object>> getTestResult(TilkoDTO certificate, HRecordDTO pDTO) throws Exception;
    String getAnalyzeResult(Map<String, Object> testResult) throws Exception;
    Boolean loginCheck(TilkoDTO pDTO) throws Exception;
}
