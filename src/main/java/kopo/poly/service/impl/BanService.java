package kopo.poly.service.impl;

import kopo.poly.dto.BanDTO;
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
        return List.of();
    }
}
