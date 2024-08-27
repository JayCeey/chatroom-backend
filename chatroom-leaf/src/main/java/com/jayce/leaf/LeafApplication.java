package com.jayce.leaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = { "com.jayce" })
@EnableFeignClients(basePackages = {"com.jayce.api.**.feign"})
public class LeafApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeafApplication.class, args);
    }
}
