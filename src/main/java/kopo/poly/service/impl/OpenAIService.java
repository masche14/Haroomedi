package kopo.poly.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.service.IOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenAIService implements IOpenAIService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)    // 연결 타임아웃
            .readTimeout(60, TimeUnit.SECONDS)       // 응답 대기 타임아웃
            .writeTimeout(60, TimeUnit.SECONDS)      // 요청 전송 타임아웃
            .callTimeout(90, TimeUnit.SECONDS)       // 전체 호출 타임아웃
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) // 커넥션 풀 설정
            .build();

    @Value("${openai.apiKey}")
    public  String openaiApiKey;

    @Override
    public Map<String, Object> getDrugSummary(String textData) throws Exception {
        log.info("{}.getDrugSummary start!", this.getClass().getName());

        // OpenAI API 요청 페이로드 설정
        String textExample = """
        {
            "drugSet": [
                {
                    "drugName": "아클펜정",
                    "drugCode": "A11AOOOOO2711",
                    "drugDetailInfo": "아세클로페낙(Aceclofenac)mg, 정제, 경구(내용고형)",
                    "effect": "류마티스관절염, 강직척추염, 골관절염, 치통, 외상 후 염증, 요통, 좌골통 등",
                    "usage": "성인: 1회 100mg, 12시간마다 1일 2회 복용",
                    "caution": "정기적인 음주자나 심혈관 질환자, 소화성궤양 병력자는 복용 주의 필요",
                    "direction": "위장장애, 음주 시 위험성, 임산부 복용 전 전문가 상담 필요 등"
                }
            ],
            "dailyIntakeCnt": 2
        }
        """;

        Map<String, Object> messageContent = new HashMap<>();
        messageContent.put("role", "user");
        messageContent.put("content", String.format("""
                %s

                해당 리스트에 있는 정보를 분석해서

                drugName 약명
                drugCode 약 코드
                drugDetailInfo 약 상세정보
                effect 효능
                usage 용법
                caution 주의사항
                direction 복약 지도

                위 내용과 관련된 데이터와

                위 리스트의 약들을 세트로 같이 섭취한다고 했을 때

                dailyIntakeCnt 처방 1일 복용 횟수
                1일 복용횟수의 경우 모든 약을 세트로 먹는 것을 1회로 가정
                의 데이터까지
                json 형태로 알려줘

                예시
                %s
                """, textData, textExample));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o");
        payload.put("messages", new Object[]{messageContent});
        payload.put("max_tokens", 3000);

        String requestBody = objectMapper.writeValueAsString(payload);

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .post(RequestBody.create(MediaType.parse("application/json"), requestBody))
                .build();

        JsonNode rootNode = null;
        int retryCount = 0;
        int maxRetries = 5;
        int baseDelayMs = 2000;  // 2초 기본 지연 시간

        while (retryCount < maxRetries) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    log.info("OpenAI API 응답: {}", responseBody);

                    rootNode = objectMapper.readTree(responseBody);
                    break;
                } else if (response.code() == 429) {
                    log.error("Rate Limit Exceeded (HTTP 429): 재시도 중...");
                    retryCount++;
                    int delay = baseDelayMs * (int) Math.pow(2, retryCount - 1);
                    log.info("대기 시간: {}ms (재시도: {}/{})", delay, retryCount, maxRetries);
                    Thread.sleep(delay);
                } else {
                    log.error("OpenAI API 호출 실패 (HTTP {}): {}", response.code(), response.message());
                    throw new Exception("OpenAI API 호출 실패");
                }
            } catch (Exception e) {
                log.error("OpenAI API 호출 중 오류 발생: {}", e.getMessage());
                if (retryCount == maxRetries) {
                    throw e;
                }
            }
        }

        if (rootNode == null) {
            throw new Exception("OpenAI 응답에서 데이터를 추출하지 못했습니다.");
        }

        String content = rootNode.path("choices").get(0).path("message").path("content").asText();
        log.info("content: {}", content);

        String jsonString = extractJsonFromResponse(content);
        if (jsonString == null) {
            throw new Exception("OpenAI 응답에서 JSON 데이터를 추출하지 못했습니다.");
        }

        Map<String, Object> result = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        log.info("result: {}", result);
        log.info("{}.getDrugSummary end!", this.getClass().getName());
        return result;
    }

    private String extractJsonFromResponse(String content) {
        Pattern pattern = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public String getAnalyzeResult(String textData) throws Exception {
        log.info("{}.getAnalyzeResult start!", this.getClass().getName());

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
