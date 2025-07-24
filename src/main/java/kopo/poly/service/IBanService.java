package kopo.poly.service;

import kopo.poly.dto.BanDTO;

import java.util.List;

public interface IBanService {
    int insertBanInfo(BanDTO pDTO);
    List<BanDTO> getBanList();
}
