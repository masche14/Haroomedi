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
    public int insertBanInfo(BanDTO pDTO) {
        return 0;
    }

    @Override
    public List<BanDTO> getBanList() {
        return List.of();
    }
}
