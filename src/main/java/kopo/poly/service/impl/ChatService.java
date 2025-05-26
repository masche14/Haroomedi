package kopo.poly.service.impl;

import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.persistance.mongodb.IChatMapper;
import kopo.poly.service.IChatService;
import kopo.poly.service.IOpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService implements IChatService {

    private final IChatMapper chatMapper;
    private final String colNm = "Chat";
    private final IOpenAIService openAIService;

    @Override
    public String insertMessage(ChatDTO chatDTO) throws Exception {
        log.info("{}.insertUserMessage Start!", this.getClass().getSimpleName());

        if (chatDTO.getSessionId() == null || chatDTO.getSessionId().isBlank()) {
            chatMapper.insertChatSession(colNm, chatDTO);
            return chatDTO.getSessionId();
        }

        for (ChatMessageDTO msg : chatDTO.getMessages()) {
            chatMapper.appendMessage(colNm, msg);
        }

        log.info("{}.insertUserMessage End!", this.getClass().getSimpleName());
        return chatDTO.getSessionId();
    }

    @Override
    public int insertBotMessage(ChatMessageDTO msgDTO) throws Exception {
        log.info("{}.insertBotMessage Start!", this.getClass().getSimpleName());
        msgDTO.setTimestamp(new Date());
        int res = chatMapper.appendMessage(colNm, msgDTO);
        log.info("{}.insertBotMessage End!", this.getClass().getSimpleName());
        return res;
    }

    @Override
    public ChatDTO getChatBySessionId(String sessionId) throws Exception {
        return chatMapper.getChatBySessionId(colNm, sessionId);
    }

    @Override
    public List<ChatDTO> getChatListByUserId(String userId) throws Exception {
        return chatMapper.getChatListByUserId(colNm, userId);
    }

    @Override
    public String getChatResponse(List<ChatMessageDTO> pList) throws Exception {

        log.info("{}.getChatResponse Start!", this.getClass().getSimpleName());

        String answer = openAIService.getChatRespose(pList);

        log.info("{}.getChatResponse End!", this.getClass().getSimpleName());

        return answer;
    }

    @Override
    public List<ChatMessageDTO> getChatMessageList(String sessionId) throws Exception {

        log.info("{}.getChatMessageList Start!", this.getClass().getSimpleName());

        List<ChatMessageDTO> rList = chatMapper.getChatMessageList(colNm, sessionId);

        log.info("{}.getChatMessageList End!", this.getClass().getSimpleName());

        return rList;
    }
}