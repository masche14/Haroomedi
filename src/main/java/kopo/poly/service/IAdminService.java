package kopo.poly.service;

import kopo.poly.dto.BanDTO;
import kopo.poly.dto.LoginDTO;
import kopo.poly.dto.UserInfoDTO;

import java.util.List;

public interface IAdminService {

    int getTodayUserCnt() throws Exception;

    List<LoginDTO> getDailyUserCntByMonth(LoginDTO pDTO) throws Exception;

    List<UserInfoDTO> getUserList() throws Exception;

    List<UserInfoDTO> updateRole(UserInfoDTO pDTO) throws Exception;

    List<UserInfoDTO> banUser(BanDTO pDTO) throws Exception;

}
