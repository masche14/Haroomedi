package kopo.poly.service;

import kopo.poly.dto.LoginDTO;

import java.util.List;

public interface ILoginService {
    int insertLoginInfo(LoginDTO pDTO) throws Exception;
    int getTodayDistinctUserCount() throws Exception;
    List<LoginDTO> getDailyDistinctUserCountByMonth(LoginDTO pDTO) throws Exception;
}
