package kopo.poly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableScheduling // ✅ 반드시 필요!
@EnableFeignClients(basePackages = "kopo.poly.feign.client") // Feign 인터페이스 패키지
public class HaroomediApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaroomediApplication.class, args);
    }

}
