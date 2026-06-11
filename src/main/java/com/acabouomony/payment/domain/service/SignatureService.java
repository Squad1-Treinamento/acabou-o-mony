package com.acabouomony.payment.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Service
public class SignatureService {

    private final Mac hmacSha256;

    public SignatureService(@Value("${webhook.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Webhook secret 'webhook.secret' cannot be empty.");
        }
        try {
            final byte[] secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            hmacSha256 = Mac.getInstance("HmacSHA256");
            hmacSha256.init(secretKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to initialize SignatureService", e);
        }
    }

    public String sign(String payload) {
        final byte[] signatureBytes = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return toHexString(signatureBytes);
    }

    private String toHexString(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}