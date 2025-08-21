package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.BanDTO;
import kopo.poly.dto.LoginDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.impl.AdminService;
import kopo.poly.util.DateUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/index")
    public String showIndexPage(HttpSession session) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null) {
            if(SS_USER.getRole().equals("user")) {
                return "user/index";
            }
        } else {
            return "user/index";
        }

        return "admin/index";
    }

    @GetMapping("/userManagement")
    public String showUserManagementPage(HttpSession session) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null) {
            if(SS_USER.getRole().equals("user")) {
                return "user/index";
            }
        } else {
            return "user/index";
        }
        return "admin/userManagement";
    }

    @GetMapping("/banUserManagement")
    public String showBanUserManagementPage(HttpSession session) {
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        if (SS_USER != null) {
            if(SS_USER.getRole().equals("user")) {
                return "user/index";
            }
        } else {
            return "user/index";
        }

        return "admin/banUserManagement";
    }

    @PostMapping("/getTodayUserCnt")
    public ResponseEntity<CommonResponse<Integer>> getTodayUserCnt(HttpSession session) throws Exception {

        log.info("{}.getTodayUserCnt", this.getClass().getName());

        int todayUserCnt = adminService.getTodayUserCnt();

        log.info("{}.getTodayUserCnt", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), todayUserCnt)
        );
    }

    @PostMapping("/getDailyUserCntByMonth")
    public ResponseEntity<CommonResponse<List<LoginDTO>>> getDailyUserCntByMonth(HttpSession session, @RequestBody LoginDTO pDTO) throws Exception {
        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        log.info("pDTO: {}", pDTO);

        List<LoginDTO> rList = adminService.getDailyUserCntByMonth(pDTO);

        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/getUserList")
    public ResponseEntity<CommonResponse<List<UserInfoDTO>>> getUserList(HttpSession session) throws Exception {

        log.info("{}.getUserList", this.getClass().getName());

        List<UserInfoDTO> rList = adminService.getUserList();

        log.info("{}.getUserList", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/getBanUserList")
    public ResponseEntity<CommonResponse<List<BanDTO>>> getBanUserList(HttpSession session) throws Exception {

        log.info("{}.getUserList", this.getClass().getName());

        List<BanDTO> rList = adminService.getUserBanList();

        log.info("{}.getUserList", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/updateRole")
    public ResponseEntity<CommonResponse<List<UserInfoDTO>>> updateRole(HttpSession session, @RequestBody UserInfoDTO pDTO) throws Exception {
        log.info("{}.updateRole Start", this.getClass().getName());

        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        pDTO.setChgId(SS_USER.getUserId());
        pDTO.setChgDt(DateUtil.getDateTime("yyyyMMddHHmmss"));

        log.info("pDTO: {}", pDTO);

        List<UserInfoDTO> rList = adminService.updateRole(pDTO);

        log.info("{}.updateRole Start", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/banUser")
    public ResponseEntity<CommonResponse<List<UserInfoDTO>>> banUser(HttpSession session, @RequestBody BanDTO pDTO) throws Exception {
        log.info("{}.banUser Start", this.getClass().getName());
        UserInfoDTO SS_USER = (UserInfoDTO) session.getAttribute("SS_USER");

        log.info("pDTO: {}", pDTO);

        pDTO.setBanBy(SS_USER.getUserId());
        pDTO.setBanAt(new Date());
        pDTO.setUserEmail(EncryptUtil.encAES128CBC(pDTO.getUserEmail()));
        pDTO.setPhoneNumber(EncryptUtil.encAES128CBC(pDTO.getPhoneNumber()));

        List<UserInfoDTO> rList = adminService.banUser(pDTO);

        log.info("{}.banUser End", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/cancelBan")
    public ResponseEntity<CommonResponse<MsgDTO>> cancelBan(HttpSession session, @RequestBody BanDTO pDTO) throws Exception {

        log.info("{}.cancelBan Start", this.getClass().getName());

        log.info("pDTO: {}", pDTO);

        MsgDTO dto = new MsgDTO();
        String msg = "차단 해제 중 오류가 발샏하였습니다.";
        int res;

        res = adminService.cancelBan(pDTO);

        if (res == 1) {
            msg = "차단이 해제되었습니다.";
        }

        dto.setMsg(msg);

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), dto)
        );
    }
}
