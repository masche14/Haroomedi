package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BanDTO {
    private String userId;
    private String userEmail;
    private String phoneNumber;
    private String reason;
    private String banAt;
}
