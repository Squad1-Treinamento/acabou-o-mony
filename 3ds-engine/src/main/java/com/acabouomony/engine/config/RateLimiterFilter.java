package com.acabouomony.engine.config;

import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
@Order(-1)
@ConditionalOnProperty(name = "3ds.rate-limit-enabled", havingValue = "true", matchIfMissing = true)
public class RateLimiterFilter implements WebFilter {

    private final int maxRequestsPerSecond;
    private final ConcurrentLinkedDeque<Long> window = new ConcurrentLinkedDeque<>();

    public RateLimiterFilter(@Value("${3ds.rate-limit-per-second:100}") int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (maxRequestsPerSecond <= 0) {
            return chain.filter(exchange);
        }

        var now = System.nanoTime();
        var windowStart = now - 1_000_000_000L;

        prune(windowStart);

        if (window.size() >= maxRequestsPerSecond) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        window.addLast(now);
        return chain.filter(exchange);
    }

    private void prune(long windowStart) {
        while (!window.isEmpty() && window.peekFirst() < windowStart) {
            window.pollFirst();
        }
    }
}
