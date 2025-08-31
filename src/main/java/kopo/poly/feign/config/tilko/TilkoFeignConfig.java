package kopo.poly.feign.config.tilko;

import feign.FeignException;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class TilkoFeignConfig {

    /**
     * - 기본적으로 API-KEY를 붙임
     * - 단, PublicKey 획득 엔드포인트(/api/Auth/GetPublicKey)는 쿼리스트링으로 APIkey를 넘기므로 헤더 생략
     * - ENC-KEY는 매 요청마다 달라서 여기서 고정 세팅하지 않음(Feign 메서드 파라미터 @RequestHeader로 전달)
     * - 본문이 있는 경우 Content-Type: application/json 추가
     */
    @Bean
    public RequestInterceptor tilkoAuthInterceptor(@Value("${tilko.apiKey}") String apiKey) {
        return template -> {
            final String url = template.url();

            template.header("Accept", "application/json");
            if (template.body() != null && !template.headers().containsKey("Content-Type")) {
                template.header("Content-Type", "application/json");
            }

            boolean isPublicKey = url != null && url.contains("/api/Auth/GetPublicKey");
            if (isPublicKey) {
                // 🔽 쿼리 파라미터 자동 추가
                if (!template.queries().containsKey("APIkey")) {
                    template.query("APIkey", apiKey);
                }
            } else {
                if (!template.headers().containsKey("API-KEY")) {
                    template.header("API-KEY", apiKey);
                }
            }
            // ENC-KEY는 각 메서드에서 @RequestHeader("ENC-KEY")로 전달
        };
    }


    @Bean
    public ErrorDecoder tilkoErrorDecoder() {
        // 필요 시 response.body()를 읽어 Tilko 에러 메시지 파싱/로깅 확장
        return (methodKey, response) -> FeignException.errorStatus(methodKey, response);
    }

    @Bean
    public Retryer tilkoRetryer() {
        // 초기 대기 100ms, 최대 1s, 최대 2회 (멱등 요청에만 권장)
        return new Retryer.Default(100, 1000, 2);
    }
}
