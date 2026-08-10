package com.nyyb.nyybserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class NyybServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NyybServerApplication.class, args);
    }

}
