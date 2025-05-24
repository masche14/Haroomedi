package kopo.poly.dto;

import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.util.Date;

@Data
public class ChatMessageDTO {
    @Transient  // MongoDB 저장 시 제외
    private String sessionId;

    private String sender;
    private String content;
    private Date timestamp;
}
