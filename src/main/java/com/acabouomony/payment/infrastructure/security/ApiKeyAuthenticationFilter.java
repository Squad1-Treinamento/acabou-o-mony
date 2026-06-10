package com.acabouomony.payment.infrastructure.security;

import com.acabouomony.payment.domain.entity.Merchant;
import com.acabouomony.payment.domain.service.MerchantAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * Spring Security filter for API key authentication.
 * 
 * Extracts API key from Authorization header and validates it using MerchantAuthService.
 * 
 * Header format: Authorization: Bearer {api_key}
 * 
 * Authentication Process:
 * 1. Extract Authorization header
 * 2. Verify it starts with "Bearer " prefix
 * 3. Extract API key from header
 * 4. Call MerchantAuthService.authenticate(apiKey)
 * 5. Service fetches all merchants and verifies key against each one
 * 6. If match found, set SecurityContext with authenticated Merchant
 * 7. If no match, return 401 Unauthorized
 * 
 * On success:
 * - Sets SecurityContext with authenticated Merchant
 * - Request proceeds to controller
 * 
 * On failure:
 * - Returns 401 Unauthorized
 * - Request does not proceed
 * 
 * Security Notes:
 * - Uses timing-safe comparison (Argon2PasswordEncoder.matches())
 * - Never logs plaintext API keys
 * - Validates each request (no caching)
 * 
 * Spec: spec-001-core-payment-processing.md - Security Rules
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final MerchantAuthService merchantAuthService;
    
    public ApiKeyAuthenticationFilter(MerchantAuthService merchantAuthService) {
        this.merchantAuthService = merchantAuthService;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract API key from Authorization header
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.debug("Request missing or invalid Authorization header");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
                return;
            }
            
            // Extract the API key (remove "Bearer " prefix)
            String apiKey = authHeader.substring(7); // "Bearer ".length()
            
            // Authenticate the API key
            Optional<Merchant> merchantOpt = merchantAuthService.authenticate(apiKey);
            
            if (!merchantOpt.isPresent()) { // Changed from isEmpty()
                logger.warn("Invalid API key provided");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            
            // On success, set the merchant in the security context
            Merchant merchant = merchantOpt.get();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                merchant, null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            logger.debug("Successfully authenticated merchant_id={}", merchant.getMerchantId());
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Error during API key authentication", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Authentication error");
        }
    }
}

