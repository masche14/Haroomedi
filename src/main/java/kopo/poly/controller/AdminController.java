package kopo.poly.controller;

import jakarta.servlet.http.HttpSession;
import kopo.poly.controller.response.CommonResponse;
import kopo.poly.dto.LoginDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IBanService;
import kopo.poly.service.ILoginService;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping(value = "/admin")
public class AdminController {
    private final IUserInfoService userInfoService;
    private final IBanService banService;
    private final ILoginService loginService;

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

        int todayUserCnt = loginService.getTodayDistinctUserCount();

        log.info("{}.getTodayUserCnt", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), todayUserCnt)
        );
    }

    @PostMapping("/getDailyUserCntByMonth")
    public ResponseEntity<CommonResponse<List<LoginDTO>>> getDailyUserCntByMonth(HttpSession session, @RequestBody LoginDTO pDTO) throws Exception {
        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        log.info("pDTO: {}", pDTO);

        List<LoginDTO> rList = loginService.getDailyDistinctUserCountByMonth(pDTO);

        log.info("{}.getDailyUserCntByMonth", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }

    @PostMapping("/getUserList")
    public ResponseEntity<CommonResponse<List<UserInfoDTO>>> getUserList(HttpSession session) throws Exception {

        log.info("{}.getUserList", this.getClass().getName());

        List<UserInfoDTO> rList = userInfoService.getUserList();

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

        int res;

        res = userInfoService.updateUserInfo(pDTO);

        List<UserInfoDTO> rList = new ArrayList<>();

        if (res > 0) {
            rList = userInfoService.getUserList();
        }

        log.info("{}.updateRole Start", this.getClass().getName());

        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, HttpStatus.OK.series().name(), rList)
        );
    }
}
