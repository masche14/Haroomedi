package kopo.poly.dto;

import lombok.Builder;

@Builder
public record MailDTO(
        String toMail,
        String title,
        String contents,
        String fromMail,
        String sendDT
) {
}
