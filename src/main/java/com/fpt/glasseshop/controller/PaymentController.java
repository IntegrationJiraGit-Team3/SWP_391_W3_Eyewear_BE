package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.OrderDTO;
import com.fpt.glasseshop.service.OrderService;
import com.fpt.glasseshop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    @PostMapping("/create_payment")
    public ResponseEntity<?> createPayment(
            @RequestParam("amount") int amount,
            @RequestParam(value = "orderInfo", defaultValue = "Thanh toan don hang") String orderInfo,
            @RequestParam(value = "bankCode", required = false) String bankCode,
            HttpServletRequest request) {

        try {
            String paymentUrl = vnPayService.createOrder(amount, orderInfo, bankCode, extractClientIp(request));

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

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? "127.0.0.1" : remoteAddr;
    }

    @GetMapping("/vnpay_return")
    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> queryParams) {
        String vnpResponseCode = queryParams.get("vnp_ResponseCode");
        String vnpTxnRef = queryParams.get("vnp_TxnRef");
        String orderInfo = queryParams.get("vnp_OrderInfo");

        String orderIdText = orderInfo != null ? orderInfo : vnpTxnRef;

        Map<String, Object> response = new HashMap<>();

        try {
            Long orderId = Long.parseLong(orderIdText.replaceAll("[^0-9]", ""));

            OrderDTO updatedOrder;
            if ("00".equals(vnpResponseCode)) {
                updatedOrder = orderService.updatePaymentOrderStatus(orderId, "PAID");
                response.put("status", "SUCCESS");
                response.put("message", "Payment for order " + orderId + " was successful");
            } else {
                updatedOrder = orderService.cancelPendingPayment(orderId);
                response.put("status", "FAILED");
                response.put("message", "Payment for order " + orderId + " failed or was cancelled");
            }

            response.put("order", updatedOrder);
            response.put("data", queryParams);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to process VNPay return: " + e.getMessage());
            response.put("data", queryParams);
            return ResponseEntity.badRequest().body(response);
        }
    }
}