package kopo.poly.service;

import kopo.poly.dto.UserInfoDTO;

import java.util.*;

public interface IUserInfoService {
    UserInfoDTO getUserIdExists(UserInfoDTO pDTO) throws Exception;
    UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception;
    int insertUserInfo(UserInfoDTO pDTO) throws Exception;
    UserInfoDTO getUserNicknameExists(UserInfoDTO pDTO) throws Exception;
    UserInfoDTO getLogin(UserInfoDTO pDTO) throws Exception;
    int updateUserInfo(UserInfoDTO pDTO) throws Exception;
    UserInfoDTO checkDuplicate(UserInfoDTO pDTO) throws Exception;
    int deleteUserInfo(UserInfoDTO pDTO) throws Exception;
    List<UserInfoDTO> getUserList() throws Exception;
}
