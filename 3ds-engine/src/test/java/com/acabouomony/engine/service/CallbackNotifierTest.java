package com.acabouomony.engine.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class CallbackNotifierTest {

    private final WebClient.Builder webClientBuilder = WebClient.builder();

    @Test
    void notifyCoreShouldReturnNonNullMono() {
        var notifier = new CallbackNotifier("http://localhost:18080", webClientBuilder);

        var result = notifier.notifyCore("ch-001", "txn-001", "merchant-1", "approved");

        assertNotNull(result, "notifyCore should return a Mono");
    }
}
