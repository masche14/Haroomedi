package kopo.poly.service.impl;

import kopo.poly.dto.LoginDTO;
import kopo.poly.persistance.mongodb.ILoginMapper;
import kopo.poly.service.ILoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginService implements ILoginService {

    private final ILoginMapper loginMapper;
    private final String colNm = "Login";

    @Override
    public int insertLoginInfo(LoginDTO pDTO) throws Exception {

        log.info("{}.insertLoginInfo Start", this.getClass().getSimpleName());

        int res = 0;

        res = loginMapper.insertLoginInfo(colNm, pDTO);

        log.info("{}.insertLoginInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public int getTodayDistinctUserCount() throws Exception {

        log.info("{}.getTodayDistinctUserCount Start", this.getClass().getSimpleName());

        int res = 0;

        res = loginMapper.getTodayDistinctUserCount(colNm);

        log.info("{}.getTodayDistinctUserCount End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public List<LoginDTO> getDailyDistinctUserCountByMonth(LoginDTO pDTO) throws Exception {

        log.info("{}.getDailyDistinctUserCountByMonth Start", this.getClass().getSimpleName());

        List<LoginDTO> rList = new ArrayList<>();

        rList = loginMapper.getDailyDistinctUserCountByMonth(colNm, pDTO);

        log.info("{}.getDailyDistinctUserCountByMonth End", this.getClass().getSimpleName());

        return rList;
    }
}
