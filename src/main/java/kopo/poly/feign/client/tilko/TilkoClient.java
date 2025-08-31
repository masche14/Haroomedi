package kopo.poly.feign.client.tilko;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "tilkoClient",
        url = "${tilko.base-url}",
        configuration = {
                kopo.poly.feign.config.tilko.TilkoFeignConfig.class,
                // ❗ FeignOkHttpConfig 패키지 경로 확인: 실제 파일 위치와 일치시킬 것
                kopo.poly.feign.config.FeignOkHttpConfig.class
        },
        contextId="tilkoClient"
)
public interface TilkoClient {

    // APIkey 파라미터 제거
    @GetMapping("/api/Auth/GetPublicKey")
    Map<String, Object> getPublicKey();

    @PostMapping(value = "/api/v1.0/nhissimpleauth/simpleauthrequest",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> simpleAuthRequest(@RequestHeader("ENC-KEY") String encKey,
                                          @RequestBody Map<String, Object> body);

    @PostMapping(value = "/api/v2.0/nhissimpleauth/LoginCheck",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> loginCheck(@RequestHeader("ENC-KEY") String encKey,
                                   @RequestBody Map<String, Object> body);

    @PostMapping(value = "/api/v1.0/nhissimpleauth/retrievetreatmentinjectioninformationperson",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> retrievePrescription(@RequestHeader("ENC-KEY") String encKey,
                                             @RequestBody Map<String, Object> body);
}
