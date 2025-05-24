package kopo.poly.service;

import java.util.Map;

public interface IOpenAIService {

    Map<String, Object> getDrugSummary(String textData) throws Exception;
    String getAnalyzeResult(String textData) throws Exception;
    String getChatRespose(String textData) throws Exception;

}
