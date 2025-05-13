package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
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
