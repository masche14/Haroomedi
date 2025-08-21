package kopo.poly.service.impl;

import kopo.poly.dto.BanDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IBanMapper;
import kopo.poly.service.IBanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class BanService implements IBanService {

    private final IBanMapper banMapper;
    private final String colNm = "Ban";

    @Override
    public int insertBanInfo(BanDTO pDTO) throws Exception {
        log.info("{}.insertBanInfo Start", this.getClass().getSimpleName());

        int res;

        res = banMapper.insertBanInfo(colNm, pDTO);

        log.info("{}.insertBanInfo End", this.getClass().getSimpleName());

        return res;
    }

    @Override
    public List<BanDTO> getBanList() throws Exception {

        log.info("{}.getBanList Start", this.getClass().getSimpleName());

        List<BanDTO> rList = banMapper.getBanList(colNm);

        log.info("{}.getBanList End", this.getClass().getSimpleName());

        return rList;
    }

    @Override
    public BanDTO isBaned(UserInfoDTO pDTO) throws Exception {

        log.info("{}.isBaned Start", this.getClass().getSimpleName());

        BanDTO rDTO = banMapper.checkIfBaned(colNm, pDTO);

        log.info("{}.isBaned End", this.getClass().getSimpleName());

        return rDTO;
    }

    @Override
    public int cancelBan(BanDTO pDTO) throws Exception {

        log.info("{}.cancelBan Start", this.getClass().getSimpleName());

        int res;

        res = banMapper.cancelBan(colNm, pDTO);

        log.info("{}.cancelBan End", this.getClass().getSimpleName());

        return res;
    }
}
