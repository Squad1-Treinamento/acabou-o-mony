package com.acabouomony.payment.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for frontend integration.
 *
 * Allows requests from the frontend development server (http://localhost:5173)
 * and configures necessary headers and methods for API communication.
 *
 * Security considerations:
 * - Only allows specific origin (localhost:5173)
 * - Credentials enabled for Authorization header
 * - Preflight cache set to 1 hour to reduce OPTIONS requests
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow frontend origin
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers (including Authorization)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList("X-Idempotent-Replayed", "Content-Type"));
        
        // Allow credentials (for Authorization header)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        
        return source;
    }
}
