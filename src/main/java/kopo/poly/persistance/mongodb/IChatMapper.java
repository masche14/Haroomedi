package kopo.poly.persistance.mongodb;

import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.List;

public interface IChatMapper {

    int insertChatSession(String colNm, ChatDTO chatDTO) throws Exception;

    int appendMessage(String colNm, ChatMessageDTO msgDTO) throws Exception;

    ChatDTO getChatBySessionId(String colNm, String sessionId) throws Exception;

    List<ChatDTO> getChatListByUserId(String colNm, String userId) throws Exception;

    List<ChatMessageDTO> getChatMessageList(String colNm, String sessionId) throws Exception;

    int deleteAllChat(String colNm, UserInfoDTO pDTO) throws Exception;

    int deleteChat(String colNm, ChatDTO pDTO) throws Exception;


}
