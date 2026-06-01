package com.acabouomony.payment.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody Object paymentRequest) {
        // TODO: Replace Object with validated DTO, implement orchestration later
        return ResponseEntity.accepted().body("Not yet implemented");
    }
}