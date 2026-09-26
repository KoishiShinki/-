package io.chronicle;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        exclude =
                org.springframework.boot.autoconfigure.security.servlet
                        .UserDetailsServiceAutoConfiguration.class)
@MapperScan("io.chronicle.timeline.mapper")
public class ChronicleApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChronicleApplication.class, args);
    }
}
