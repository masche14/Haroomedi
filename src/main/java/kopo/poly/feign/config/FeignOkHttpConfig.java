package kopo.poly.feign.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class FeignOkHttpConfig {

    /**
     * Feign이 내부 HTTP 클라이언트로 사용할 OkHttpClient.
     * application.properties 의 spring.cloud.openfeign.okhttp.enabled=true 필요.
     */
    @Bean
    public OkHttpClient feignOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)             // 연결 타임아웃
                .readTimeout(180, TimeUnit.SECONDS)               // 응답 대기 타임아웃
                .writeTimeout(180, TimeUnit.SECONDS)              // 요청 전송 타임아웃
                .callTimeout(240, TimeUnit.SECONDS)               // 전체 호출 타임아웃
                .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES)) // 커넥션 풀
                .retryOnConnectionFailure(true)                   // 연결 실패 시 자동 재시도
                .build();
    }
}