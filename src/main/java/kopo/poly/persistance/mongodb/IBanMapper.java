package kopo.poly.persistance.mongodb;

import kopo.poly.dto.BanDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.List;

public interface IBanMapper {
    int insertBanInfo(String colNm, BanDTO pDTO) throws Exception;
    List<BanDTO> getBanList(String colNm) throws Exception;
    BanDTO checkIfBaned(String colNm, UserInfoDTO pDTO) throws Exception;
    int cancelBan(String colNm, BanDTO pDTO) throws Exception;
}
