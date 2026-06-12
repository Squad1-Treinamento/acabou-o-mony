package com.acabouomony;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class PaymentCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentCoreApplication.class, args);
    }
}
