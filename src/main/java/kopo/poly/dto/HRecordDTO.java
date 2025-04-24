package kopo.poly.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class HRecordDTO {
    private String userId;
    private int seq;
    private LocalDate date;
    private String location;
    private String code;
    private String description;
}
