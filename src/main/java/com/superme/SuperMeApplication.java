package com.superme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableScheduling
@EntityScan(basePackages = {
        "com.superme.model",
        "com.superme.admin.model"
})
public class SuperMeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuperMeApplication.class, args);
    }
}


