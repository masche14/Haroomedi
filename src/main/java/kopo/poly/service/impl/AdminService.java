package kopo.poly.service.impl;

import kopo.poly.dto.BanDTO;
import kopo.poly.dto.LoginDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService implements IAdminService {
    private final IUserInfoService userInfoService;
    private final IBanService banService;
    private final ILoginService loginService;
    private final IHealthService healthService;
    private final IChatService chatService;

    @Override
    public int getTodayUserCnt() throws Exception {

        log.info("{}.getTodayUserCnt", this.getClass().getName());

        int res = loginService.getTodayDistinctUserCount();

        log.info("{}.getTodayUserCnt", this.getClass().getName());

        return res;
    }

    @Override
    public List<LoginDTO> getDailyUserCntByMonth(LoginDTO pDTO) throws Exception {

        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        log.info("pDTO: {}", pDTO);

        List<LoginDTO> rList = loginService.getDailyDistinctUserCountByMonth(pDTO);

        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        return rList;
    }

    @Override
    public List<UserInfoDTO> getUserList() throws Exception {

        log.info("{}.getUserList", this.getClass().getName());

        List<UserInfoDTO> rList = userInfoService.getUserList();

        log.info("{}.getUserList", this.getClass().getName());

        return rList;
    }

    @Override
    public List<UserInfoDTO> updateRole(UserInfoDTO pDTO) throws Exception {

        log.info("{}.updateRole Start", this.getClass().getName());

        int res;

        res = userInfoService.updateUserInfo(pDTO);

        List<UserInfoDTO> rList = new ArrayList<>();

        if (res > 0) {
            rList = userInfoService.getUserList();
        }

        log.info("{}.updateRole Start", this.getClass().getName());

        return rList;
    }

    @Override
    public List<UserInfoDTO> banUser(BanDTO pDTO) throws Exception {

        log.info("{}.banUser Start", this.getClass().getName());

        int banRes;
        int deleteRes;
        List<UserInfoDTO> rList = new ArrayList<>();

        banRes = banService.insertBanInfo(pDTO);

        if (banRes > 0) {
            log.info("차단 완료");
            UserInfoDTO userInfoDTO = new UserInfoDTO();
            userInfoDTO.setUserId(pDTO.getUserId());
            deleteRes = userInfoService.deleteUserInfo(userInfoDTO);
            if (deleteRes > 0) {
                int delPrescription = healthService.deleteAllPrescription(userInfoDTO);
                int delReminder = healthService.deleteAllReminder(userInfoDTO);
                int delChat = chatService.deleteAllChat(userInfoDTO);

                if (delPrescription > 0 && delReminder > 0 && delChat > 0) {
                    log.info("유저 정보 삭제 완료");
                }

                rList = userInfoService.getUserList();
            }
        }

        log.info("{}.banUser End", this.getClass().getName());

        return rList;
    }
}
