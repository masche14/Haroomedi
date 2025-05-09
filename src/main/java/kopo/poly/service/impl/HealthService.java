package kopo.poly.service.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.HRecordDTO;
import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.TilkoDTO;
import kopo.poly.persistance.mongodb.IHealthMapper;
import kopo.poly.service.IHealthService;
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
import java.time.LocalDate;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class HealthService implements IHealthService {
    private static final String API_HOST = "https://api.tilko.net/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // 연결 대기 시간
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)     // 읽기 대기 시간
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)    // 쓰기 대기 시간
            .build();
    private final IHealthMapper healthMapper;

    @Value("${tilko.apiKey}")
    public  String tilkoApiKey;

    @Value("${openai.apiKey}")
    public  String openaiApiKey;

    @Override
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

    @Override
    public String encryptAES(SecretKey aesKey, IvParameterSpec iv, String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    @Override
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

        String rsaPublicKey = getPublicKey();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);

        String aesCipherKey = encryptRSA(rsaPublicKey, aesKey.getEncoded());

        // 두 번째 API 요청 처리
        String url = API_HOST + "api/v1.0/NhisSimpleAuth/Ggpab003M0105";

        Map<String, Object> secondRequestBody = new HashMap<>();
        secondRequestBody.put("CxId", certificateResult.getCxId());
        secondRequestBody.put("PrivateAuthType", certificateResult.getPrivateAuthType());
        secondRequestBody.put("ReqTxId", certificateResult.getReqTxId());
        secondRequestBody.put("Token", certificateResult.getToken());
        secondRequestBody.put("TxId", certificateResult.getTxId());
        secondRequestBody.put("UserName", encryptAES(aesKey, iv, certificateResult.getUserName()));
        secondRequestBody.put("BirthDate", encryptAES(aesKey, iv, certificateResult.getBirthDate()));
        secondRequestBody.put("UserCellphoneNumber", encryptAES(aesKey, iv, certificateResult.getUserCellphoneNumber()));
        secondRequestBody.put("기타필요한파라미터", "");

        RequestBody secondBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(secondRequestBody));
        Request secondRequest = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("API-KEY", tilkoApiKey)
                .header("ENC-KEY", aesCipherKey)
                .post(secondBody)
                .build();

        Response secondResponse = client.newCall(secondRequest).execute();
        Map<String, Object> secondResponseMap = objectMapper.readValue(secondResponse.body().string(), Map.class);
        List<Map<String, Object>> ResultList = (List<Map<String, Object>>) secondResponseMap.get("ResultList");
        log.info("ResultList : {}", ResultList);

        PrescriptionDTO dDTO = healthMapper.getLatestRecord(certificateResult);
        PrescriptionDTO rDTO = new PrescriptionDTO();

        List<Map<String, Object>> testResultList = new ArrayList<>();
        List<PrescriptionDTO> rList = new ArrayList<>();

        if (dDTO==null){

            for (Map<String, Object> resultMap : ResultList) {
                Map<String, Object> healthMap = new HashMap<>();
                List<Map<String, Object>> health = new ArrayList<>();

                String year = (String) resultMap.get("Year");
                year = year.replace("년","");
                log.info("year: {}", year);

                String checkupDate = (String) resultMap.get("CheckUpDate");
                String month = checkupDate.split("/")[0];
                String day = checkupDate.split("/")[1];
                log.info("month: {}", month);
                log.info("day: {}", day);

                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
                log.info("date: {}", date);
                rDTO.setPrescriptionDate(date);

                String location = (String) resultMap.get("Location");
                log.info("location: {}", location);
                rDTO.setStoreName(location);

                int success = healthMapper.insertRecord(rDTO);

                if (success == 1){
                    log.info("레코드 데이터를 성공적으로 저장하였습니다.");
                }

                List<Map<String, Object>> inspections = (List<Map<String, Object>>) resultMap.get("Inspections");

                for (Map<String, Object> inspection : inspections) {

                    List<Map<String, Object>> illnesses = (List<Map<String, Object>>) inspection.get("Illnesses");
                    for (Map<String, Object> illness : illnesses) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) illness.get("Items");
                        for (Map<String, Object> item : items) {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("검사명", (String) item.get("Name"));
                            entry.put("결과", (String) item.get("Value"));
                            health.add(entry);
//                            healthMap.put((String) item.get("Name"), item.get("Value"));
                        }
                    }
                }

                healthMap.put("year", year);
                healthMap.put("result", health);

                testResultList.add(healthMap);
            }

        } else {

            LocalDate latestDate = rDTO.getPrescriptionDate();

            log.info("latestDate: {}", latestDate);

            for (Map<String, Object> resultMap : ResultList) {
                Map<String, Object> healthMap = new HashMap<>();
                List<Map<String, Object>> health = new ArrayList<>();

                String year = (String) resultMap.get("Year");
                year = year.replace("년","");
                log.info("year: {}", year);

                String checkupDate = (String) resultMap.get("CheckUpDate");
                String month = checkupDate.split("/")[0];
                String day = checkupDate.split("/")[1];
                log.info("month: {}", month);
                log.info("day: {}", day);

                LocalDate date = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
                log.info("date: {}", date);

                String location = (String) resultMap.get("Location");
                log.info("location: {}", location);
                rDTO.setStoreName(location);

                if (date.isAfter(latestDate)) {

                    rDTO.setPrescriptionDate(date);

                    int success = healthMapper.insertRecord(rDTO);

                    if (success == 1){
                        log.info("레코드 데이터를 성공적으로 저장하였습니다.");
                    }

                    List<Map<String, Object>> inspections = (List<Map<String, Object>>) resultMap.get("Inspections");

                    for (Map<String, Object> inspection : inspections) {
                        List<Map<String, Object>> illnesses = (List<Map<String, Object>>) inspection.get("Illnesses");
                        for (Map<String, Object> illness : illnesses) {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) illness.get("Items");
                            for (Map<String, Object> item : items) {
                                Map<String, Object> entry = new HashMap<>();
                                entry.put("검사명", (String) item.get("Name"));
                                entry.put("결과", (String) item.get("Value"));
                                health.add(entry);
                                healthMap.put((String) item.get("Name"), item.get("Value"));
                            }
                        }
                    }

                    healthMap.put("year", year);
                    healthMap.put("result", health);

                    testResultList.add(healthMap);
                } else {
                    log.info("이미 해당 날짜의 검사결과 데이터가 존재합니다.");
                }

            }

        }

        log.info("{}.getTestResult end!", this.getClass().getName());
        return rList;
    }

    @Override
    public String getAnalyzeResult(Map<String, Object> testResult) throws Exception {
        log.info("{}.getAnalyzeResult start!", this.getClass().getName());

        String textData = testResult.toString();
        log.info("textData: {}", textData);

        Map<String, Object> openAiPayload = new HashMap<>();
        openAiPayload.put("model", "gpt-4o");
        openAiPayload.put("max_tokens", 3000);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "다음 건강검진 결과를 분석하고 이상 수치가 있는지 확인한 후, 건강 상태를 평가해 주세요. 만약 검사내역이 2회 이상일 경우 분석과 평가 모두 각각 나눠서 해주세요. :\n" + textData);
        messages.add(message);
        openAiPayload.put("messages", messages);

        RequestBody openAiBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(openAiPayload));
        Request openAiRequest = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .post(openAiBody)
                .build();

        Response openAiResponse = client.newCall(openAiRequest).execute();
        Map<String, Object> openAiResponseMap = objectMapper.readValue(openAiResponse.body().string(), Map.class);

        // `choices` 배열에서 첫 번째 항목 가져오기
        List<Map<String, Object>> choices = (List<Map<String, Object>>) openAiResponseMap.get("choices");
        String content = "";
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> resultMessage = (Map<String, Object>) firstChoice.get("message");

            if (message != null) {
                content = (String) resultMessage.get("content");
                log.info("건강검진 분석결과 :\n" + content);
            } else {
                log.info("Message object is null.");
            }
        } else {
            log.info("Choices array is empty or null.");
        }

        log.info("{}.getAnalyzeResult end!", this.getClass().getName());
        return content;
    }
}
