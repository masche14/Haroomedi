package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.*;

@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class LoginDTO {
    // 로그인 기록
    private String userId;
    private String userName;
    private String role;
    private Date loginAt;

    // 일자별 통계용
    private String loginDateString; // 예: "2025-07-24"
    private int userCount;          // 해당 날짜의 접속자 수 (distinct userId 수)

    // 조회 조건
    private Date startDate; // 예: 2025-07-01
    private Date endDate;   // 예: 2025-08-01
}
