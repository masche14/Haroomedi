package kopo.poly.service.impl;

import kopo.poly.persistance.mongodb.IUserInfoMapper;
import kopo.poly.service.IBanService;
import kopo.poly.service.ILoginService;
import kopo.poly.service.IUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AdminService {
    private final IUserInfoService userInfoService;
    private final ILoginService loginService;
    private final IBanService banService;
}
