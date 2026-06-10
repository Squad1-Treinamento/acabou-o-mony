package com.acabouomony.payment.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // <-- Added missing import
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for API key authentication.
 *
 * Configures:
 * - Stateless session management (no cookies)
 * - API key authentication filter
 * - Argon2 password encoder
 *
 * Spec: spec-001-core-payment-processing.md - Security Rules
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    public SecurityConfig(ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) {
        this.apiKeyAuthenticationFilter = apiKeyAuthenticationFilter;
    }

    /**
     * Configures the security filter chain.
     *
     * - Disables CSRF (stateless API)
     * - Sets session creation policy to STATELESS (no cookies)
     * - Requires authentication for /api/v1/payments endpoints
     * - Allows health check endpoints without authentication
     * - Registers API key authentication filter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Modern Lambda style for disabling CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Modern Lambda style for Session Management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 3. Modern Lambda style for Authorization Request Rules
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .anyRequest().permitAll()
                )

                // 4. Custom filters remain configured similarly
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /*
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
    */

    /**
     * Configures Argon2 password encoder.
     *
     * Parameters:
     * - saltLength: 16 bytes
     * - hashLength: 32 bytes
     * - parallelism: 1
     * - memory: 65536 KB (64 MB)
     * - iterations: 3
     *
     * This provides strong hashing with high cost factor (2^16 iterations minimum).
     */
    @Bean
    public Argon2PasswordEncoder argon2PasswordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}

