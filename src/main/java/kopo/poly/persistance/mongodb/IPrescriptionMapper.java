package kopo.poly.persistance.mongodb;

import kopo.poly.dto.PrescriptionDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.List;

public interface IPrescriptionMapper {
    int insertPrescriptionInfo(String colNm, List<PrescriptionDTO> pList) throws Exception;

    List<PrescriptionDTO> getPrescriptionList(String colNm, UserInfoDTO pDTO) throws Exception;

    PrescriptionDTO getLatestPrescriptionInfo(String colNm, UserInfoDTO pDTO) throws Exception;

    int updatePrescriptionInfo(String colNm, PrescriptionDTO pDTO) throws Exception;

    PrescriptionDTO getPrescriptionById(String colNm, PrescriptionDTO pDTO) throws Exception;
}
