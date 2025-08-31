package kopo.poly.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.*;
import kopo.poly.feign.client.tilko.TilkoClient;
import kopo.poly.persistance.mongodb.IPrescriptionMapper;
import kopo.poly.persistance.mongodb.IReminderMapper;
import kopo.poly.service.IHealthService;
import kopo.poly.service.IOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class HealthService implements IHealthService {

    private final IOpenAIService openAIService;

    private final IPrescriptionMapper prescriptionMapper;
    private final IReminderMapper reminderMapper;

    // ✅ Feign Client 주입
    private final TilkoClient tilkoClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /* =========================
       Tilko 공용 유틸
       ========================= */
    public String getPublicKey() throws Exception {
        // 인터셉터가 ?APIkey= 자동 추가 → 파라미터 없이 호출
        Map<String, Object> res = tilkoClient.getPublicKey();
        Object key = (res != null) ? res.get("PublicKey") : null;
        if (key == null) throw new RuntimeException("Failed to get public key");
        return String.valueOf(key);
    }

    public String encryptAES(SecretKey aesKey, IvParameterSpec iv, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String encryptRSA(String publicKeyStr, byte[] aesKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(publicKeyStr);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey);
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /* =========================
       Tilko 연동 (Feign)
       ========================= */

    @Override
    public TilkoDTO getCertificateResult(TilkoDTO pDTO) throws Exception {
        log.info("{}.getCertificateResult start!", this.getClass().getName());

        // 1) 공개키/AES/ENC-KEY 준비
        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        String encKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        // 2) 요청 바디 (민감정보 AES 암호화)
        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("PrivateAuthType", pDTO.getPrivateAuthType());
        jsonBody.put("UserName", encryptAES(aesKey, iv, pDTO.getUserName()));
        jsonBody.put("BirthDate", encryptAES(aesKey, iv, pDTO.getBirthDate()));
        jsonBody.put("UserCellphoneNumber", encryptAES(aesKey, iv, pDTO.getUserCellphoneNumber()));

        // 3) 호출 (API-KEY는 인터셉터, ENC-KEY는 헤더 인자)
        Map<String, Object> res = tilkoClient.simpleAuthRequest(encKey, jsonBody);

        // 4) ResultData -> TilkoDTO
        TilkoDTO certificateResult = objectMapper.convertValue(res.get("ResultData"), TilkoDTO.class);

        log.info("{}.getCertificateResult End!", this.getClass().getName());
        return certificateResult;
    }

    @Override
    public Boolean loginCheck(TilkoDTO pDTO) throws Exception {
        // 1) 공개키/AES/ENC-KEY
        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        String encKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        // 2) 요청 바디
        Map<String, Object> auth = new HashMap<>();
        auth.put("CxId", pDTO.getCxId());
        auth.put("PrivateAuthType", pDTO.getPrivateAuthType());
        auth.put("ReqTxId", pDTO.getReqTxId());
        auth.put("Token", pDTO.getToken());
        auth.put("TxId", pDTO.getTxId());
        auth.put("UserName", encryptAES(aesKey, iv, pDTO.getUserName()));
        auth.put("BirthDate", encryptAES(aesKey, iv, pDTO.getBirthDate()));
        auth.put("UserCellphoneNumber", encryptAES(aesKey, iv, pDTO.getUserCellphoneNumber()));

        Map<String, Object> body = new HashMap<>();
        body.put("Auth", auth);

        // 3) 호출
        Map<String, Object> res = tilkoClient.loginCheck(encKey, body);

        // 4) 결과 파싱
        Object result = res.get("Result");
        return (result instanceof Boolean) ? (Boolean) result : Boolean.valueOf(String.valueOf(result));
    }

    @Override
    public int synchronizePrescriptions(TilkoDTO certificateResult) throws Exception {
        log.info("{}.synchronizePrescriptions start!", this.getClass().getName());

        int res = 0;

        // 1) 공개키/AES/ENC-KEY
        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        String encKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        // 2) 요청 바디
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("CxId", certificateResult.getCxId());
        requestBody.put("PrivateAuthType", certificateResult.getPrivateAuthType());
        requestBody.put("ReqTxId", certificateResult.getReqTxId());
        requestBody.put("Token", certificateResult.getToken());
        requestBody.put("TxId", certificateResult.getTxId());
        requestBody.put("UserName", encryptAES(aesKey, iv, certificateResult.getUserName()));
        requestBody.put("BirthDate", encryptAES(aesKey, iv, certificateResult.getBirthDate()));
        requestBody.put("UserCellphoneNumber", encryptAES(aesKey, iv, certificateResult.getUserCellphoneNumber()));
        requestBody.put("기타필요한파라미터", "");

        // 3) 호출
        Map<String, Object> responseMap = tilkoClient.retrievePrescription(encKey, requestBody);

        // 4) 결과 파싱
        List<Map<String, Object>> resultList =
                objectMapper.convertValue(responseMap.get("ResultList"),
                        new TypeReference<List<Map<String, Object>>>() {});

        log.info("API 처방데이터 개수 : {}", (resultList == null) ? 0 : resultList.size());
        if (resultList == null || resultList.isEmpty()) return 0;

        List<PrescriptionDTO> prescriptionList = new ArrayList<>();

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserId(certificateResult.getUserId());
        String colNm = "Prescription";

        PrescriptionDTO latestDTO = prescriptionMapper.getLatestPrescriptionInfo(colNm, pDTO);

        for (Map<String, Object> result : resultList) {
            try {
                String jinRyoHyungTae = (String) result.get("JinRyoHyungTae");
                if (!"처방조제".equals(jinRyoHyungTae)) continue;

                String storeName = (String) result.get("ByungEuiwonYakGukMyung");
                String prescriptionDate = String.valueOf(result.get("JinRyoGaesiIl"));
                String period = String.valueOf(result.get("TuYakYoYangHoiSoo"));
                int prescriptionPeriod = Integer.parseInt(period);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                Date date = formatter.parse(prescriptionDate);

                if (latestDTO != null) {
                    boolean older = date.compareTo(latestDTO.getPrescriptionDate()) < 0;
                    boolean same = date.compareTo(latestDTO.getPrescriptionDate()) == 0
                            && storeName.equals(latestDTO.getStoreName());
                    if (older || same) {
                        log.info("DB의 최신 데이터와 일치합니다.");
                        break;
                    }
                }

                List<Map<String, Object>> detailList =
                        (List<Map<String, Object>>) result.get("RetrieveTreatmentInjectionInformationPersonDetailList");
                if (detailList == null || detailList.isEmpty()) continue;

                List<Map<String, Object>> drugList = new ArrayList<>();
                for (Map<String, Object> detail : detailList) {
                    Map<String, Object> detailInfo = (Map<String, Object>) detail.get("RetrieveMdsupDtlInfo");
                    if (detailInfo != null) {
                        try { detailInfo.remove("DrugImage"); } catch (Exception ignore) {}
                        drugList.add(detailInfo);
                    }
                }

                String textData = objectMapper.writeValueAsString(drugList);
                Map<String, Object> summaryResult = openAIService.getDrugSummary(textData);

                PrescriptionDTO prescription = new PrescriptionDTO();
                prescription.setUserId(certificateResult.getUserId());
                prescription.setPrescriptionDate(date);
                prescription.setStoreName(storeName);
                prescription.setPrescriptionPeriod(prescriptionPeriod);
                prescription.setDrugList((List<Map<String, Object>>) summaryResult.get("drugSet"));
                prescription.setRemindYn("N");

                prescriptionList.add(prescription);

            } catch (Exception e) {
                log.error("Error processing prescription: {}", e.getMessage(), e);
            }
        }

        log.info("추가할 최신 데이터 개수 : {}", prescriptionList.size());
        if (!prescriptionList.isEmpty()) {
            res = prescriptionMapper.insertPrescriptionInfo(colNm, prescriptionList);
        }

        log.info("{}.synchronizePrescriptions end!", this.getClass().getName());
        return res;
    }

    /* =========================
       이하 Mongo 연동 (기존 유지)
       ========================= */

    @Override
    public List<PrescriptionDTO> getPrescriptionList(UserInfoDTO pDTO) throws Exception {
        log.info("{}.getPrescriptionList Start!", this.getClass().getName());
        String colNm = "Prescription";
        List<PrescriptionDTO> prescriptionList = prescriptionMapper.getPrescriptionList(colNm, pDTO);
        log.info("{}.getPrescriptionList End!", this.getClass().getName());
        return prescriptionList;
    }

    @Override
    public PrescriptionDTO updatePrescriptionInfo(PrescriptionDTO pDTO) throws Exception {
        log.info("{}.updatePrescriptionInfo Start!", this.getClass().getName());
        String colNm = "Prescription";
        PrescriptionDTO rDTO = null;
        int res = prescriptionMapper.updatePrescriptionInfo(colNm, pDTO);
        if (res > 0) {
            rDTO = prescriptionMapper.getPrescriptionById(colNm, pDTO);
        }
        log.info("{}.updatePrescriptionInfo End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public int insertReminder(ReminderDTO pDTO) throws Exception {
        log.info("{}.insertReminder Start!", this.getClass().getName());
        String colNm = "Reminder";
        int res = reminderMapper.insertReminder(colNm, pDTO);
        log.info("{}.insertReminder End!", this.getClass().getName());
        return res;
    }

    @Override
    public int deleteReminder(PrescriptionDTO pDTO) throws Exception {
        log.info("{}.deleteReminder Start!", this.getClass().getName());
        String colNm = "Reminder";
        int res = reminderMapper.deleteReminder(colNm, pDTO);
        log.info("{}.deleteReminder End!", this.getClass().getName());
        return res;
    }

    @Override
    public int updateReminderMealTime(UserInfoDTO pDTO) throws Exception {
        log.info("{}.updateReminderMealTime Start!", this.getClass().getName());
        String colNm = "Reminder";
        int res = reminderMapper.updateMealTime(colNm, pDTO);
        log.info("{}.updateReminderMealTime End!", this.getClass().getName());
        return res;
    }

    public int updatePrescriptionAndReminderUserId(UserInfoDTO pDTO) throws Exception {
        log.info("{}.updatePrescriptionAndReminderUserId Start!", this.getClass().getName());
        String colNm1 = "Prescription";
        String colNm2 = "Reminder";
        int success1 = prescriptionMapper.updateUserId(colNm1, pDTO);
        int success2 = reminderMapper.updateUserId(colNm2, pDTO);
        log.info("{}.updatePrescriptionAndReminderUserId End!", this.getClass().getName());
        return (success1 == 1 && success2 == 1) ? 1 : 0;
    }

    @Override
    public ReminderDTO getReminderByPrescriptionId(ReminderDTO pDTO) throws Exception {
        log.info("{}.getReminderByPrescriptionId Start!", this.getClass().getName());
        String colNm = "Reminder";
        ReminderDTO rDTO = reminderMapper.getReminderByPrescriptionId(colNm, pDTO);
        log.info("{}.getReminderByPrescriptionId End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public int updateIntakeLog(ReminderDTO pDTO) throws Exception {
        log.info("{}.updateIntakeLog Start", this.getClass().getSimpleName());
        String colNm = "Reminder";

        int res = 0;
        List<Map<String, Object>> intakeLogList = pDTO.getIntakeLog();
        if (intakeLogList == null || intakeLogList.isEmpty()) {
            log.warn("intakeLog가 비어있음");
            return res;
        }

        Map<String, Object> intakeLogEntry = intakeLogList.get(0);
        String intakeTimeStr = String.valueOf(intakeLogEntry.get("intakeTime")); // "2025-05-19T07:30"
        String intakeYn = String.valueOf(intakeLogEntry.get("intakeYn"));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Date intakeTime = sdf.parse(intakeTimeStr);

        Map<String, Object> convertedEntry = new HashMap<>();
        convertedEntry.put("intakeTime", intakeTime);
        convertedEntry.put("intakeYn", intakeYn);

        List<Map<String, Object>> newLogList = new ArrayList<>();
        newLogList.add(convertedEntry);
        pDTO.setIntakeLog(newLogList);

        int success = reminderMapper.updateIntakeLog(colNm, pDTO);
        if (success > 1) res = 1;

        log.info("{}.updateIntakeLog End", this.getClass().getSimpleName());
        return res;
    }

    @Override
    public int deleteAllPrescription(UserInfoDTO pDTO) throws Exception {
        log.info("{}.deleteAllPrescription Start!", this.getClass().getName());
        String colNm = "Prescription";
        int success = prescriptionMapper.deleteAllPrescription(colNm, pDTO);
        int res = success > 0 ? 1 : 0;
        log.info("{}.deleteAllPrescription End", this.getClass().getName());
        return res;
    }

    @Override
    public int deleteAllReminder(UserInfoDTO pDTO) throws Exception {
        log.info("{}.deleteAllReminder Start", this.getClass().getName());
        String colNm = "Reminder";
        int success = reminderMapper.deleteAllReminder(colNm, pDTO);
        int res = success > 0 ? 1 : 0;
        log.info("{}.deleteAllReminder End", this.getClass().getName());
        return res;
    }
}
