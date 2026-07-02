package server.sassedo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SassedoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SassedoApplication.class, args);
    }

}
