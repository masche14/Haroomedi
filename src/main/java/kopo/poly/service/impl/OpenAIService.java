package kopo.poly.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.feign.client.openai.OpenAIClient;
import kopo.poly.service.IOpenAIService;
import kopo.poly.util.MarkdownUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import feign.FeignException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
@Service
public class OpenAIService implements IOpenAIService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAIClient openAIClient; // ✅ Feign 주입

    @Override
    public Map<String, Object> getDrugSummary(String textData) throws Exception {
        log.info("{}.getDrugSummary start!", this.getClass().getName());

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

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", String.format("""
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
        payload.put("messages", List.of(userMessage));
        payload.put("max_tokens", 3000);

        Map<String, Object> response = callWith429Retry(payload);

        String content = extractChoiceContent(response);
        log.info("content: {}", content);

        String jsonString = extractJsonFromResponse(content);
        if (jsonString == null) {
            throw new Exception("OpenAI 응답에서 JSON 데이터를 추출하지 못했습니다.");
        }

        Map<String, Object> result =
                objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        log.info("result: {}", result);
        log.info("{}.getDrugSummary end!", this.getClass().getName());
        return result;
    }

    @Override
    public String getChatRespose(List<ChatMessageDTO> pList) throws Exception {
        log.info("{}.getChatRespose start!", this.getClass().getName());

        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content", "당신은 약사입니다. 사용자의 증상에 따라 일반의약품(OTC) 중 적절한 약을 추천하고, 필요 시 병원 방문도 권유하세요."
        ));

        for (ChatMessageDTO dto : pList) {
            String role = dto.getSender().equalsIgnoreCase("USER") ? "user" : "assistant";
            messages.add(Map.of("role", role, "content", dto.getContent()));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "gpt-4o");
        payload.put("messages", messages);
        payload.put("max_tokens", 3000);

        Map<String, Object> response = callWith429Retry(payload);
        String content = extractChoiceContent(response);

        // Markdown → HTML 후 UI용 치환
        content = MarkdownUtil.toSafeHtml(content);
        content = content.replaceAll(":\\s*", " ");

        log.info("{}.getChatResponse end!", this.getClass().getName());
        return content;
    }

    /* ---------- 공통 유틸 ---------- */

    private Map<String, Object> callWith429Retry(Map<String, Object> payload) throws Exception {
        int retryCount = 0;
        int maxRetries = 5;
        int baseDelayMs = 2000; // 2s

        while (true) {
            try {
                return openAIClient.chatCompletions(payload);
            } catch (FeignException e) {
                if (e.status() == 429 && retryCount < maxRetries) {
                    retryCount++;
                    int delay = baseDelayMs * (int) Math.pow(2, retryCount - 1);
                    log.warn("OpenAI 429 Too Many Requests → {}ms 대기 후 재시도 ({}/{})", delay, retryCount, maxRetries);
                    Thread.sleep(delay);
                } else {
                    log.error("OpenAI 호출 실패 (status={}): {}", e.status(), e.getMessage());
                    throw e;
                }
            }
        }
    }

    private String extractChoiceContent(Map<String, Object> response) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) return "";
        Map<String, Object> first = choices.get(0);
        Map<String, Object> message = (Map<String, Object>) first.get("message");
        return message == null ? "" : String.valueOf(message.get("content"));
    }

    private String extractJsonFromResponse(String content) {
        Pattern pattern = Pattern.compile("```json\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }
}
