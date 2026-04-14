package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.service.VNPayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class PaymentController {

    @Autowired
    private VNPayService vnPayService;

    @PostMapping("/create_payment")
    public ResponseEntity<?> createPayment(
            @RequestParam("amount") int amount,
            @RequestParam(value = "orderInfo", defaultValue = "Thanh toan don hang") String orderInfo) {
        
        try {
            String paymentUrl = vnPayService.createOrder(amount, orderInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("message", "Successfully created payment URL");
            response.put("paymentUrl", paymentUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Failed to create payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> queryParams) {
        // Validation logic for the vnp_SecureHash from queryParams should go here.
        // For now, this just echoes back the response to confirm the redirect works.
        String vnpResponseCode = queryParams.get("vnp_ResponseCode");
        String vnpTxnRef = queryParams.get("vnp_TxnRef");
        
        Map<String, Object> response = new HashMap<>();
        if ("00".equals(vnpResponseCode)) {
            response.put("status", "SUCCESS");
            response.put("message", "Payment for order " + vnpTxnRef + " was successful");
            response.put("data", queryParams);
        } else {
            response.put("status", "FAILED");
            response.put("message", "Payment for order " + vnpTxnRef + " failed or was cancelled");
            response.put("data", queryParams);
        }
        
        return ResponseEntity.ok(response);
    }
}
