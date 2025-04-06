package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Builder
public record MelonDTO(
        @NotBlank(message="수집 시간은 필수 입력 사항입니다.")
        String collectTime,
        String song,
        String singer,
        int singerCnt,
        String updateSinger,
        String nickname,
        List<String> member,
        String addFieldValue
) {
}
