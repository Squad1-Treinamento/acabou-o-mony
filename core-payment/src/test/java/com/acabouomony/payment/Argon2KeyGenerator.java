package com.acabouomony.payment;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public class Argon2KeyGenerator {
    public static void main(String[] args) {
        String plainApiKey = "teste_key";

        // Use the Argon2 encoder, matching the application's config
        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
        String hashedPassword = encoder.encode(plainApiKey);

        System.out.println("====================================================================");
        System.out.println("Plain API Key (Use this in Bruno): " + plainApiKey);
        System.out.println("Hashed API Key (Use this in the database): " + hashedPassword);
        System.out.println("====================================================================");
    }
}