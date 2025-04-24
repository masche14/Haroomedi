package kopo.poly.service.impl;

import kopo.poly.dto.MailDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.persistance.mongodb.IUserInfoMapper;
import kopo.poly.service.IMailService;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserInfoService implements IUserInfoService {

    private final IUserInfoMapper userInfoMapper;
    private final IMailService mailService;

    @Override
    public UserInfoDTO getUserIdExists(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserIdExists Start", this.getClass().getName());

        String colNm = "UserInfo";

        UserInfoDTO rDTO = userInfoMapper.checkFieldExists(colNm, pDTO);

        return rDTO;
    }

    @Override
    public UserInfoDTO checkDuplicate(UserInfoDTO pDTO) throws Exception {
        log.info("{}.checkDuplicate Start", this.getClass().getName());
        String colNm = "UserInfo";
        UserInfoDTO rDTO = userInfoMapper.checkFieldExists(colNm, pDTO);
        log.info("{}.checkDuplicate End", this.getClass().getName());
        return rDTO;
    }

    @Override
    public UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserEmailExists Start", this.getClass().getName());

        String colNm = "UserInfo";

        UserInfoDTO rDTO = userInfoMapper.checkFieldExists(colNm, pDTO);

        log.info("rDTO : {}", rDTO.toString());

        if (rDTO.getExistYn().equals("Y")) {
            UserInfoDTO rDTO2 = userInfoMapper.getUserIdAndUserNameByUserEmail(colNm, pDTO);
            rDTO.setUserId(rDTO2.getUserId());
            rDTO.setUserName(rDTO2.getUserName());
        }

        log.info("rDTO: {}", rDTO.toString());

        int authNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);

        log.info("authNumber : {}", authNumber);

        MailDTO dto = new MailDTO();

        dto.setTitle("이메일 확인 인증번호 발송 메일");
        dto.setContents("인증번호는 " + authNumber + " 입니다.");
        dto.setToMail(EncryptUtil.decAES128CBC(CmmUtil.nvl(rDTO.getUserEmail())));

        log.info("dto: {}", dto.toString());

        mailService.doSendMail(dto);

        dto=null;

        rDTO.setAuthNumber(authNumber);

        log.info("{}.getUserEmailExists End", this.getClass().getName());

        return rDTO;
    }

    @Override
    public int insertUserInfo(UserInfoDTO pDTO) throws Exception {

        log.info("{}.insertUserInfo", this.getClass().getName());

        String colNm = "UserInfo";

        int res;

        int success = userInfoMapper.insertUserInfo(colNm, pDTO);

        if (success > 0) {
            res = 1;
            MailDTO mDTO = new MailDTO();

            mDTO.setToMail(EncryptUtil.decAES128CBC(pDTO.getUserEmail()));
            mDTO.setTitle("회원가입을 축하드립니다.");
            mDTO.setContents(CmmUtil.nvl(pDTO.getUserName())+"님의 회원가입을 진심으로 축하드립니다.");

            mailService.doSendMail(mDTO);

        } else {
            res = 0;
        }

        log.info("{}.insertUserInfo End", this.getClass().getName());

        return res;
    }

    @Override
    public UserInfoDTO getUserNicknameExists(UserInfoDTO pDTO) throws Exception {

        String colNm = "UserInfo";

        UserInfoDTO rDTO = userInfoMapper.checkFieldExists(colNm, pDTO);

        return rDTO;
    }

    @Override
    public UserInfoDTO getLogin(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getLogin Start", this.getClass().getName());

        String colNm = "UserInfo";

        UserInfoDTO rDTO = userInfoMapper.getLogin(colNm, pDTO);

        log.info("{}.getLogin End", this.getClass().getName());

        return rDTO;
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
