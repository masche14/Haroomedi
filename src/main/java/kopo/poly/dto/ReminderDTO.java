package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Data
public class ReminderDTO {
    private String prescriptionId;
    private String userId;
    private List<String> mealTime;
    private int toIntakeCnt;
    private int dailyToIntakeCnt;
    private int intakeCnt;
    private int leftIntakeCnt;
    List<Map<String,Object>> intakeLog;
}
