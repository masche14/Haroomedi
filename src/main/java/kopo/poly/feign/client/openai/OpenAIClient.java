package kopo.poly.feign.client.openai;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
        name = "openaiClient",
        url = "${openai.base-url}",
        configuration = {
                kopo.poly.feign.config.openai.OpenAIFeignConfig.class,
                kopo.poly.feign.config.FeignOkHttpConfig.class
        },
        contextId = "openAIClient"
)
public interface OpenAIClient {

    @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    Map<String, Object> chatCompletions(@RequestBody Map<String, Object> body);
}
