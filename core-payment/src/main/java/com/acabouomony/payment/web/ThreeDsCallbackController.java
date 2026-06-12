package com.acabouomony.payment.web;

import com.acabouomony.payment.domain.service.PaymentOrchestrationService;
import com.acabouomony.payment.web.dto.ThreeDsCallbackRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class ThreeDsCallbackController {

    private final PaymentOrchestrationService orchestrationService;

    public ThreeDsCallbackController(PaymentOrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping("/3ds-callback")
    public ResponseEntity<Void> callback(@RequestBody ThreeDsCallbackRequest request) {
        boolean approved = "approved".equals(request.getAuthStatus());
        orchestrationService.completeThreeDsAuthentication(UUID.fromString(request.getTransactionId()), approved);
        return ResponseEntity.ok().build();
    }
}
