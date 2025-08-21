package kopo.poly.service;

import kopo.poly.dto.BanDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.List;

public interface IBanService {
    int insertBanInfo(BanDTO pDTO) throws Exception;
    List<BanDTO> getBanList() throws Exception;
    BanDTO isBaned(UserInfoDTO pDTO) throws Exception;
    int cancelBan(BanDTO pDTO) throws Exception;
}
