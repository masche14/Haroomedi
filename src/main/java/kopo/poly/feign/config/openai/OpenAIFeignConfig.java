package kopo.poly.feign.config.openai;

import feign.FeignException;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class OpenAIFeignConfig {

    @Bean
    public RequestInterceptor openAiAuthInterceptor(@Value("${openai.apiKey}") String apiKey) {
        return template -> {
            template.header("Accept", "application/json");
            if (template.body() != null && !template.headers().containsKey("Content-Type")) {
                template.header("Content-Type", "application/json");
            }
            if (!template.headers().containsKey("Authorization")) {
                template.header("Authorization", "Bearer " + apiKey);
            }
        };
    }

    @Bean
    public ErrorDecoder openAiErrorDecoder() {
        return (methodKey, response) -> FeignException.errorStatus(methodKey, response);
    }

    @Bean
    public Retryer openAiRetryer() {
        // 재시도는 서비스에서 429만 지수백오프로 처리하므로, 여기서는 최소 수준으로 두거나 0회도 OK
        return new Retryer.Default(100, 1000, 0);
    }
}
