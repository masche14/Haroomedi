package kopo.poly.persistance.mongodb;

import kopo.poly.dto.LoginDTO;

import java.util.List;

public interface ILoginMapper {
    int insertLoginInfo(String colNm, LoginDTO pDTO) throws Exception;
    // 오늘의 접속자 수 조회
    int getTodayDistinctUserCount(String colNm) throws Exception;
    // 선택한 달의 일별 접속자 수 조회
    List<LoginDTO> getDailyDistinctUserCountByMonth(String colNm, LoginDTO pDTO) throws Exception;
}
