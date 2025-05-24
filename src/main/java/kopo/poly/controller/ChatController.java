package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.ChatDTO;
import kopo.poly.dto.ChatMessageDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/chat")
public class ChatController {

    private final IChatService chatService;

    @GetMapping("/chat")
    public String chat(HttpSession session) {
        return "/chat/chat";
    }

    @PostMapping("/send")
    public ResponseEntity<ChatMessageDTO> sendUserMessage(@RequestBody ChatMessageDTO pDTO, HttpSession session) throws Exception {
        String userId = ((UserInfoDTO) session.getAttribute("SS_USER")).getUserId();
        pDTO.setTimestamp(new Date());

        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setUserId(userId);
        chatDTO.setMessages(List.of(pDTO));

        if (pDTO.getSessionId() == null || pDTO.getSessionId().isBlank()) {
            chatDTO.setStartAt(new Date());
            chatDTO.setSummary(pDTO.getContent());
            chatDTO.setEndAt(pDTO.getTimestamp());
        } else {
            chatDTO.setSessionId(pDTO.getSessionId());
        }

        String sessionId = chatService.insertMessage(chatDTO);
        pDTO.setSessionId(sessionId);

        return ResponseEntity.ok(pDTO);
    }


    @GetMapping("/messages")
    public ResponseEntity<ChatDTO> getMessages(@RequestParam String sessionId) throws Exception {
        ChatDTO rDTO = chatService.getChatBySessionId(sessionId);
        return ResponseEntity.ok(rDTO);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ChatDTO>> getChatList(HttpSession session) throws Exception {
        String userId = ((UserInfoDTO) session.getAttribute("SS_USER")).getUserId();
        List<ChatDTO> rList = chatService.getChatListByUserId(userId);
        return ResponseEntity.ok(rList);
    }

    @PostMapping("/aiRequest")
    public ResponseEntity<ChatMessageDTO> aiRequest(@RequestBody ChatMessageDTO pDTO, HttpSession session) throws Exception {
        String userId = ((UserInfoDTO) session.getAttribute("SS_USER")).getUserId();

        // ✅ AI 응답 요청 및 저장
        String aiContent = chatService.getChatResponse(pDTO.getContent());
        ChatMessageDTO botMsg = new ChatMessageDTO();
        botMsg.setSessionId(pDTO.getSessionId());
        botMsg.setSender("BOT");
        botMsg.setContent(aiContent);
        botMsg.setTimestamp(new Date());

        ChatDTO chatDTO = new ChatDTO();
        chatDTO.setUserId(userId);
        chatDTO.setMessages(List.of(botMsg));
        chatDTO.setSessionId(pDTO.getSessionId());

        String sessionId = chatService.insertMessage(chatDTO);
        botMsg.setSessionId(sessionId);

        return ResponseEntity.ok(botMsg);
    }

}
