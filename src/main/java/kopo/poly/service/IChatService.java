package kopo.poly.service;

import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;

import java.util.List;

public interface IChatService {

    String insertMessage(ChatDTO chatDTO) throws Exception;

    int insertBotMessage(ChatMessageDTO msgDTO) throws Exception;

    ChatDTO getChatBySessionId(String sessionId) throws Exception;

    List<ChatDTO> getChatListByUserId(String userId) throws Exception;

    String getChatResponse(List<ChatMessageDTO> pList) throws Exception;

    List<ChatMessageDTO> getChatMessageList(String sessionId) throws Exception;


}
