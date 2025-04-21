package kopo.poly.service.impl;

import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IUserInfoMapper;
import kopo.poly.service.IUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {

    private final IUserInfoMapper userInfoMapper;

    @Override
    public UserInfoDTO getUserIdExists(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserIdExists Start", this.getClass().getName());

        String colNm = "UserInfo";

        String existYn = userInfoMapper.checkFieldExists(colNm, pDTO)?"Y":"N";

        UserInfoDTO rDTO = UserInfoDTO.builder().existsYn(existYn).build();

        return rDTO;
    }

    @Override
    public UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserEmailExists Start", this.getClass().getName());

        String colNm = "UserInfo";

        String existYn = userInfoMapper.checkFieldExists(colNm, pDTO)?"Y":"N";

        UserInfoDTO rDTO = UserInfoDTO.builder().existsYn(existYn).build();



        return rDTO;
    }

    @Override
    public int insertUserInfo(UserInfoDTO pDTO) throws Exception {
        return 0;
    }

    @Override
    public UserInfoDTO getUserNicknameExists(UserInfoDTO pDTO) throws Exception {

        String colNm = "UserInfo";

        String existYn = userInfoMapper.checkFieldExists(colNm, pDTO)?"Y":"N";

        UserInfoDTO rDTO = UserInfoDTO.builder().existsYn(existYn).build();

        return rDTO;
    }

    @Override
    public UserInfoDTO getLogin(UserInfoDTO pDTO) throws Exception {
        return null;
    }

    @Override
    public int updateUserInfo(UserInfoDTO pDTO) throws Exception {
        return 0;
    }

    @Override
    public int deleteUserInfo(UserInfoDTO pDTO) throws Exception {
        return 0;
    }
}
