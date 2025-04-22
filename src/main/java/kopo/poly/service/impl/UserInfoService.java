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
    public UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception {

        log.info("{}.getUserEmailExists Start", this.getClass().getName());

        String colNm = "UserInfo";

        UserInfoDTO rDTO = userInfoMapper.checkFieldExists(colNm, pDTO);

        log.info("rDTO : {}", rDTO);

        if (rDTO.existsYn().equals("Y")) {
            UserInfoDTO rDTO2 = userInfoMapper.getUserIdAndUserNameByUserEmail(colNm, pDTO);

            // 새로운 DTO 생성 (필요한 필드 조합)
            UserInfoDTO finalDTO = UserInfoDTO.builder()
                    .existsYn(rDTO.existsYn())
                    .userEmail(rDTO.userEmail()) // 필요하면 유지
                    .userName(rDTO2.userName())
                    .userId(rDTO2.userId())
                    .build();

            // 기존 rDTO를 교체
            rDTO = finalDTO;
        }

        log.info("rDTO: {}", rDTO);

        int authNumber = ThreadLocalRandom.current().nextInt(100000, 1000000);

        log.info("authNumber : {}", authNumber);

        MailDTO dto = MailDTO.builder().build();

//        dto.setTitle("이메일 확인 인증번호 발송 메일");
//        dto.setContents("인증번호는 " + authNumber + " 입니다.");
//        dto.setToMail(EncryptUtil.decAES128CBC(CmmUtil.nvl(pDTO.getUserEmail())));

        dto.builder().title("이메일 확인 인증번호 발송 메일")
                .contents("인증번호는 " + authNumber + " 입니다.")
                .toMail(EncryptUtil.decAES128CBC(CmmUtil.nvl(pDTO.userEmail())))
                .build();

        mailService.doSendMail(dto);

        dto=null;

        rDTO.builder().authNumber(authNumber).build();

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
            MailDTO mDTO = MailDTO.builder().build();

            mDTO.builder().toMail(EncryptUtil.decAES128CBC(pDTO.userEmail()))
                    .title("회원가입을 축하드립니다.").build();


            mDTO.builder().contents(CmmUtil.nvl(pDTO.userName())+"님의 회원가입을 진심으로 축하드립니다.").build();

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
