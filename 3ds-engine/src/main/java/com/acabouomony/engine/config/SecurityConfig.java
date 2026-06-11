package com.acabouomony.engine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(name = "3ds.security.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityConfig {

    private static final String ACTUATOR_PREFIX = "/actuator";

    @Value("${3ds.api-key}")
    private String apiKey;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(e -> e.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .build();
    }

    @Bean
    WebFilter apiKeyFilter() {
        return (exchange, chain) -> {
            if (isPublicPath(exchange)) {
                return chain.filter(exchange);
            }
            return validateApiKey(exchange)
                    .switchIfEmpty(chain.filter(exchange));
        };
    }

    private boolean isPublicPath(ServerWebExchange exchange) {
        var path = exchange.getRequest().getURI().getPath();
        return (exchange.getRequest().getMethod() == HttpMethod.GET
                && path.startsWith("/challenge/"))
                || path.startsWith(ACTUATOR_PREFIX);
    }

    private Mono<Void> validateApiKey(ServerWebExchange exchange) {
        var provided = exchange.getRequest().getHeaders().getFirst("X-API-Key");
        if (provided != null && provided.equals(apiKey)) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
