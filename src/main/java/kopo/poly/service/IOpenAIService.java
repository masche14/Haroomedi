package kopo.poly.service;

import kopo.poly.dto.ChatMessageDTO;

import java.util.List;
import java.util.Map;

public interface IOpenAIService {

    Map<String, Object> getDrugSummary(String textData) throws Exception;
    String getAnalyzeResult(String textData) throws Exception;
    String getChatRespose(List<ChatMessageDTO> pList) throws Exception;

}
