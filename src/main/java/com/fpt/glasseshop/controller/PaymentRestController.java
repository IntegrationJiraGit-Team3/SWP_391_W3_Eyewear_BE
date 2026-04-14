package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.Payment;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.PaymentRequest;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "PaymentAPI", description = "Operations related to payment processing")
public class PaymentRestController {

    private final PaymentService paymentService;

    // ✅ Lấy email từ JWT
    private String getCurrentUserEmail() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return email;
    }

    @PostMapping("/process")
    @Operation(summary = "Process order payment", description = "Simulates payment processing and updates order status")
    public ResponseEntity<ApiResponse<Payment>> processPayment(
            @Valid @RequestBody PaymentRequest req) {

        try {
            String userEmail = getCurrentUserEmail();

            Payment payment = paymentService.processPayment(req, userEmail);

            if ("SUCCESS".equals(payment.getStatus())) {
                return ResponseEntity.ok(
                        ApiResponse.success("Payment processed successfully", payment)
                );
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Payment failed", payment));
            }

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Unexpected error: " + e.getMessage()));
        }
    }
}