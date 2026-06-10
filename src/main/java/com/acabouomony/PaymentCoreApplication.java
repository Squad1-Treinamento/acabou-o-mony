package com.acabouomony;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentCoreApplication {
    public static void main(String[] args) {
        System.out.println("--- RUNNING WITH JAVA VERSION: " + System.getProperty("java.version") + " ---");
        SpringApplication.run(PaymentCoreApplication.class, args);
    }
}
