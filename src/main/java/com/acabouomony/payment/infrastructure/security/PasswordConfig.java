package com.acabouomony.payment.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        // As per task-004-merchant-authentication.md
        // saltLength=16, hashLength=32, memory=65536, iterations=3, parallelism=1
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}