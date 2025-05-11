package kopo.poly.dto;

import lombok.Data;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.*;

@Data
public class PrescriptionDTO {
    private String prescriptionId;
    private String userId;
    private Date prescriptionDate;
    private String storeName;
    private int prescriptionPeriod;
    private List<Map<String, Object>> drugList;
    private int dailyIntakeCnt;
    private String remindYn;

}
