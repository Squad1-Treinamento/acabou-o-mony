package com.acabouomony.payment.infrastructure.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for authentication testing.
 * 
 * Provides a protected endpoint that requires authentication.
 * Only active in test profile.
 */
@RestController
@RequestMapping("/api/v1/test")
@Profile("test")
public class TestAuthController {
    
    /**
     * Protected endpoint that requires authentication.
     * Returns the authenticated merchant ID.
     */
    @GetMapping("/protected")
    public Map<String, String> protectedEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, String> response = new HashMap<>();
        
        if (auth != null && auth.isAuthenticated()) {
            response.put("status", "authenticated");
            response.put("merchantId", auth.getPrincipal().toString());
        } else {
            response.put("status", "unauthenticated");
        }
        
        return response;
    }
}
