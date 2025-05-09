package kopo.poly.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.*;

@Data
public class PrescriptionDTO {

    private String userId;
    private LocalDate prescriptionDate;
    private String storeName;
    private int prescriptionPeriod;
    private List<Map<String, Object>> drugList;
    private int toIntakeCnt;
    private String remindYn;

}
