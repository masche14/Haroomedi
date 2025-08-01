package kopo.poly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ 반드시 필요!
public class HaroomediApplication {

    public static void main(String[] args) {
        SpringApplication.run(HaroomediApplication.class, args);
    }

}
