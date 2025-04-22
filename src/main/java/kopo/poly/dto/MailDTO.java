package kopo.poly.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class MailDTO{
    private String toMail;
    private String title;
    private String contents;
    private String fromMail;
    private String sendDT;
}
