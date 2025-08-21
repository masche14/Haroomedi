package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BanDTO {
    private String userId;
    private String userName;
    private String userEmail;
    private String phoneNumber;
    private String reason;
    private String banBy;
    private Date banAt;
    private String existYn;
}
