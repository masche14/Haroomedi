package kopo.poly.service.impl;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.TilkoDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IHealthMapper;
import kopo.poly.persistance.mongodb.IPrescriptionMapper;
import kopo.poly.service.IHealthService;
import kopo.poly.service.IOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class HealthService implements IHealthService {

    private final IOpenAIService openAIService;

    private static final String API_HOST = "https://api.tilko.net/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)    // 연결 타임아웃 (기존 30초 → 60초)
            .readTimeout(180, TimeUnit.SECONDS)      // 응답 대기 타임아웃 (기존 60초 → 180초)
            .writeTimeout(180, TimeUnit.SECONDS)     // 요청 전송 타임아웃 (기존 60초 → 180초)
            .callTimeout(240, TimeUnit.SECONDS)      // 전체 호출 타임아웃 (기존 90초 → 240초)
            .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES)) // 커넥션 풀 크기 증가
            .retryOnConnectionFailure(true)          // 연결 실패 시 자동 재시도
            .build();

    private final IHealthMapper healthMapper;
    private final IPrescriptionMapper prescriptionMapper;

    @Value("${tilko.apiKey}")
    public  String tilkoApiKey;

    @Value("${openai.apiKey}")
    public  String openaiApiKey;

    public String getPublicKey() throws Exception {
        Request request = new Request.Builder()
                .url(API_HOST + "/api/Auth/GetPublicKey?APIkey=" + tilkoApiKey)
                .header("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new RuntimeException("Failed to get public key");
        Map<String, String> responseMap = objectMapper.readValue(response.body().string(), Map.class);
        return responseMap.get("PublicKey");
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

    @Override
    public TilkoDTO getCertificateResult(TilkoDTO pDTO) throws Exception {
        log.info("{}.getCertificateResult start!", this.getClass().getName());
        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        String aesCipherKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        Map<String, Object> jsonBody = new HashMap<>();
        jsonBody.put("PrivateAuthType", pDTO.getPrivateAuthType());
        jsonBody.put("UserName", encryptAES(aesKey, iv, pDTO.getUserName()));
        jsonBody.put("BirthDate", encryptAES(aesKey, iv, pDTO.getBirthDate()));
        jsonBody.put("UserCellphoneNumber", encryptAES(aesKey, iv, pDTO.getUserCellphoneNumber()));

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(jsonBody));
        Request request = new Request.Builder()
                .url(API_HOST + "api/v1.0/nhissimpleauth/simpleauthrequest")
                .header("Content-Type", "application/json")
                .header("API-KEY", tilkoApiKey)
                .header("ENC-KEY", aesCipherKey)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();

        JsonNode rootNode = objectMapper.readTree(response.body().string());
        JsonNode resultDataNode = rootNode.get("ResultData");

        TilkoDTO certificateResult = objectMapper.treeToValue(resultDataNode, TilkoDTO.class);

        log.info("{}.getCertificateResult End!", this.getClass().getName());

        return certificateResult;
    }

    @Override
    public Boolean loginCheck(TilkoDTO pDTO) throws Exception {

        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        String aesCipherKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        String url = API_HOST + "api/v2.0/nhissimpleauth/LoginCheck";

        Map<String, Object> loginCheckRequestBody = new HashMap<>();
        Map<String, Object> Auth = new HashMap<>();

        Auth.put("CxId", pDTO.getCxId());
        Auth.put("PrivateAuthType", pDTO.getPrivateAuthType());
        Auth.put("ReqTxId", pDTO.getReqTxId());
        Auth.put("Token", pDTO.getToken());
        Auth.put("TxId", pDTO.getTxId());
        Auth.put("UserName", encryptAES(aesKey, iv, pDTO.getUserName()));
        Auth.put("BirthDate", encryptAES(aesKey, iv, pDTO.getBirthDate()));
        Auth.put("UserCellphoneNumber", encryptAES(aesKey, iv, pDTO.getUserCellphoneNumber()));

        loginCheckRequestBody.put("Auth", Auth);

        log.info("loginCheckRequestBody : {}", loginCheckRequestBody.toString());

        RequestBody loginCheckBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(loginCheckRequestBody));
        Request loginCheckRequest = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("API-KEY", tilkoApiKey)
                .header("ENC-KEY", aesCipherKey)
                .post(loginCheckBody)
                .build();

        log.info("loginCheckBody : {}",loginCheckBody.toString());

        Response loginCheckResponse = client.newCall(loginCheckRequest).execute();
        Map<String, Object> loginCheckResponseMap = objectMapper.readValue(loginCheckResponse.body().string(), Map.class);
        log.info("loginCheckResult: " + loginCheckResponseMap);


        Boolean result = (Boolean) loginCheckResponseMap.get("Result");
        log.info("result : {}",result.toString());

        return result;
    }

    @Transactional
    @Override
    public List<PrescriptionDTO> getTestResult(TilkoDTO certificateResult) throws Exception {
        log.info("{}.getTestResult start!", this.getClass().getName());

        List<PrescriptionDTO> rList = new ArrayList<>();

//        String rsaPublicKey = getPublicKey();
//        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//        keyGen.init(128);
//        SecretKey aesKey = keyGen.generateKey();
//        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
//
//        String aesCipherKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());
//
//        // 두 번째 API 요청 처리
//        String url = API_HOST + "api/v1.0/NhisSimpleAuth/Ggpab003M0105";
//
//        Map<String, Object> secondRequestBody = new HashMap<>();
//        secondRequestBody.put("CxId", certificateResult.getCxId());
//        secondRequestBody.put("PrivateAuthType", certificateResult.getPrivateAuthType());
//        secondRequestBody.put("ReqTxId", certificateResult.getReqTxId());
//        secondRequestBody.put("Token", certificateResult.getToken());
//        secondRequestBody.put("TxId", certificateResult.getTxId());
//        secondRequestBody.put("UserName", encryptAES(aesKey, iv, certificateResult.getUserName()));
//        secondRequestBody.put("BirthDate", encryptAES(aesKey, iv, certificateResult.getBirthDate()));
//        secondRequestBody.put("UserCellphoneNumber", encryptAES(aesKey, iv, certificateResult.getUserCellphoneNumber()));
//        secondRequestBody.put("기타필요한파라미터", "");
//
//        RequestBody secondBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(secondRequestBody));
//        Request secondRequest = new Request.Builder()
//                .url(url)
//                .header("Content-Type", "application/json")
//                .header("API-KEY", tilkoApiKey)
//                .header("ENC-KEY", aesCipherKey)
//                .post(secondBody)
//                .build();
//
//        Response secondResponse = client.newCall(secondRequest).execute();
//        Map<String, Object> secondResponseMap = objectMapper.readValue(secondResponse.body().string(), Map.class);
//        List<Map<String, Object>> ResultList = (List<Map<String, Object>>) secondResponseMap.get("ResultList");
//        log.info("ResultList : {}", ResultList);
//
//        PrescriptionDTO dDTO = healthMapper.getLatestRecord(certificateResult);
//        PrescriptionDTO rDTO = new PrescriptionDTO();
//
//        List<Map<String, Object>> testResultList = new ArrayList<>();
//        List<PrescriptionDTO> rList = new ArrayList<>();
//
//        if (dDTO==null){
//
//            for (Map<String, Object> resultMap : ResultList) {
//                Map<String, Object> healthMap = new HashMap<>();
//                List<Map<String, Object>> health = new ArrayList<>();
//
//                String year = (String) resultMap.get("Year");
//                year = year.replace("년","");
//                log.info("year: {}", year);
//
//                String checkupDate = (String) resultMap.get("CheckUpDate");
//                String month = checkupDate.split("/")[0];
//                String day = checkupDate.split("/")[1];
//                log.info("month: {}", month);
//                log.info("day: {}", day);
//
//                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
//                log.info("date: {}", date);
//                rDTO.setPrescriptionDate(date);
//
//                String location = (String) resultMap.get("Location");
//                log.info("location: {}", location);
//                rDTO.setStoreName(location);
//
//                int success = healthMapper.insertRecord(rDTO);
//
//                if (success == 1){
//                    log.info("레코드 데이터를 성공적으로 저장하였습니다.");
//                }
//
//                List<Map<String, Object>> inspections = (List<Map<String, Object>>) resultMap.get("Inspections");
//
//                for (Map<String, Object> inspection : inspections) {
//
//                    List<Map<String, Object>> illnesses = (List<Map<String, Object>>) inspection.get("Illnesses");
//                    for (Map<String, Object> illness : illnesses) {
//                        List<Map<String, Object>> items = (List<Map<String, Object>>) illness.get("Items");
//                        for (Map<String, Object> item : items) {
//                            Map<String, Object> entry = new HashMap<>();
//                            entry.put("검사명", (String) item.get("Name"));
//                            entry.put("결과", (String) item.get("Value"));
//                            health.add(entry);
////                            healthMap.put((String) item.get("Name"), item.get("Value"));
//                        }
//                    }
//                }
//
//                healthMap.put("year", year);
//                healthMap.put("result", health);
//
//                testResultList.add(healthMap);
//            }
//
//        } else {
//
//            LocalDate latestDate = rDTO.getPrescriptionDate();
//
//            log.info("latestDate: {}", latestDate);
//
//            for (Map<String, Object> resultMap : ResultList) {
//                Map<String, Object> healthMap = new HashMap<>();
//                List<Map<String, Object>> health = new ArrayList<>();
//
//                String year = (String) resultMap.get("Year");
//                year = year.replace("년","");
//                log.info("year: {}", year);
//
//                String checkupDate = (String) resultMap.get("CheckUpDate");
//                String month = checkupDate.split("/")[0];
//                String day = checkupDate.split("/")[1];
//                log.info("month: {}", month);
//                log.info("day: {}", day);
//
//                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
//                log.info("date: {}", date);
//
//                String location = (String) resultMap.get("Location");
//                log.info("location: {}", location);
//                rDTO.setStoreName(location);
//
//                if (date.isAfter(latestDate)) {
//
//                    rDTO.setPrescriptionDate(date);
//
//                    int success = healthMapper.insertRecord(rDTO);
//
//                    if (success == 1){
//                        log.info("레코드 데이터를 성공적으로 저장하였습니다.");
//                    }
//
//                    List<Map<String, Object>> inspections = (List<Map<String, Object>>) resultMap.get("Inspections");
//
//                    for (Map<String, Object> inspection : inspections) {
//                        List<Map<String, Object>> illnesses = (List<Map<String, Object>>) inspection.get("Illnesses");
//                        for (Map<String, Object> illness : illnesses) {
//                            List<Map<String, Object>> items = (List<Map<String, Object>>) illness.get("Items");
//                            for (Map<String, Object> item : items) {
//                                Map<String, Object> entry = new HashMap<>();
//                                entry.put("검사명", (String) item.get("Name"));
//                                entry.put("결과", (String) item.get("Value"));
//                                health.add(entry);
//                                healthMap.put((String) item.get("Name"), item.get("Value"));
//                            }
//                        }
//                    }
//
//                    healthMap.put("year", year);
//                    healthMap.put("result", health);
//
//                    testResultList.add(healthMap);
//                } else {
//                    log.info("이미 해당 날짜의 검사결과 데이터가 존재합니다.");
//                }
//
//            }
//
//        }

        log.info("{}.getTestResult end!", this.getClass().getName());
        return rList;
    }

    @Override
    public String getAnalyzeResult(Map<String, Object> testResult) throws Exception {
        log.info("{}.getAnalyzeResult start!", this.getClass().getName());

        String textData = testResult.toString();
        log.info("textData: {}", textData);

        String content = openAIService.getAnalyzeResult(textData);

        log.info("{}.getAnalyzeResult end!", this.getClass().getName());
        return content;
    }

    @Override
    public int synchronizePrescriptions(TilkoDTO certificateResult) throws Exception {
        log.info("{}.getPrescriptionList start!", this.getClass().getName());

        int res = 0;

        // AES 키 및 IV 생성
        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        // AES 암호화 키 생성
        String aesCipherKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        // API URL 설정
        String url = API_HOST + "api/v1.0/nhissimpleauth/retrievetreatmentinjectioninformationperson";

        // 요청 파라미터 설정
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

        // API 요청
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(requestBody));
        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("API-KEY", tilkoApiKey)
                .header("ENC-KEY", aesCipherKey)
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        Map<String, Object> responseMap = objectMapper.readValue(response.body().string(), new TypeReference<>() {});
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) responseMap.get("ResultList");

        log.info("API 처방데이터 개수 : {}", resultList.size());

        List<PrescriptionDTO> prescriptionList = new ArrayList<>();

        UserInfoDTO pDTO = new UserInfoDTO();
        pDTO.setUserId(certificateResult.getUserId());
        String colNm = "Prescription";

        PrescriptionDTO latestDTO = prescriptionMapper.getLatestPrescriptionInfo(colNm, pDTO);

        for (Map<String, Object> result : resultList) {
            try {
                // 처방 유형 필터
                String jinRyoHyungTae = (String) result.get("JinRyoHyungTae");
                if (!"처방조제".equals(jinRyoHyungTae)) {
                    continue;
                }

                String storeName = (String) result.get("ByungEuiwonYakGukMyung");
                String prescriptionDate = (String) result.get("JinRyoGaesiIl");
                String period = (String) result.get("TuYakYoYangHoiSoo");
                int prescriptionPeriod = Integer.parseInt(period);

                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                Date date = formatter.parse(prescriptionDate);

                if (latestDTO != null) {
                    if (date.compareTo(latestDTO.getPrescriptionDate())<0 || (date.compareTo(latestDTO.getPrescriptionDate())==0&&storeName.equals(latestDTO.getStoreName()))) {
                        log.info("DB의 최신 데이터와 일치합니다.");
                        break;
                    }
                }

                log.info("DB에 존재하지 않는 최신 데이터 입니다.");

                List<Map<String, Object>> detailList = (List<Map<String, Object>>) result.get("RetrieveTreatmentInjectionInformationPersonDetailList");

                if (detailList.isEmpty()){
                    continue;
                }

                List<Map<String, Object>> drugList = new ArrayList<>();

                for (Map<String, Object> detail : detailList) {
                    Map<String, Object> detailInfo = (Map<String, Object>) detail.get("RetrieveMdsupDtlInfo");
                    try {
                        detailInfo.remove("DrugImage");
                    } catch (Exception e) {
                        // 아무 작업도 하지 않음 (pass와 동일)
                    }
                    drugList.add(detailInfo);
                }

                // 약물 정보 요약 요청
                String textData = objectMapper.writeValueAsString(drugList);

                log.info("textData : {}", textData);

                Map<String, Object> summaryResult = openAIService.getDrugSummary(textData);

                // 처방 데이터 구성
                PrescriptionDTO prescription = new PrescriptionDTO();
                prescription.setUserId(certificateResult.getUserId());
                prescription.setPrescriptionDate(date);  // 날짜 변환 필요
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

        if (!prescriptionList.isEmpty()){
            res = prescriptionMapper.insertPrescriptionInfo(colNm, prescriptionList);
        }

        log.info("{}.getPrescriptionList end!", this.getClass().getName());

        return res;
    }

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
        if (res>0){
            rDTO = prescriptionMapper.getPrescriptionById(colNm, pDTO);
        }

        log.info("{}.updatePrescriptionInfo End!", this.getClass().getName());

        return rDTO;
    }
}

