package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Data
public class ChatDTO {

    private String sessionId;
    private String userId;
    private Date startAt;
    private Date endAt;
    private String summary;

    private List<ChatMessageDTO> messages;
}
