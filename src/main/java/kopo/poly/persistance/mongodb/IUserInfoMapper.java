package kopo.poly.persistance.mongodb;

import kopo.poly.dto.UserInfoDTO;

public interface IUserInfoMapper {

    int insertUserInfo(String colNm, UserInfoDTO pDTO) throws Exception;

    UserInfoDTO checkFieldExists(String colNm, UserInfoDTO pDTO);

//    UserInfoDTO getUserIdExists(String colNm, UserInfoDTO pDTO) throws Exception;
//
//    UserInfoDTO getUserNicknameExists(String colNm, UserInfoDTO pDTO) throws Exception;
//
//    UserInfoDTO getUserEmailExists(String colNm, UserInfoDTO pDTO) throws Exception;

    UserInfoDTO getLogin(String colNm, UserInfoDTO pDTO) throws Exception;

    int updateUserInfo(String colNm, UserInfoDTO pDTO) throws Exception;

    int deleteUserInfo(String colNm, UserInfoDTO pDTO) throws Exception;
}
