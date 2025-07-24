package kopo.poly.persistance.mongodb;

import kopo.poly.dto.BanDTO;

import java.util.List;

public interface IBanMapper {
    int insertBanInfo(String colNm, BanDTO pDTO) throws Exception;
    List<BanDTO> getBanList(String colNm) throws Exception;
}
