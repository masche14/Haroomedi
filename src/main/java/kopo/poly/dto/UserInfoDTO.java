package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record UserInfoDTO(
        String userId,
        String userName,
        String password,
        String userEmail,
        String userNickname,
        String regId,
        String regDt,
        String chgId,
        String chgDt,
        String gender,
        String existsYn,
        String fieldName,
        String value,
        int authNumber
) {}
