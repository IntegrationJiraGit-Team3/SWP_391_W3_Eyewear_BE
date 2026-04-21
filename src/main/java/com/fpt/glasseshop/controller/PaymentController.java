package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.Payment;
import com.fpt.glasseshop.entity.dto.OrderDTO;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.PaymentRepository;
import com.fpt.glasseshop.service.OrderService;
import com.fpt.glasseshop.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

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
        String vnpPayDate = queryParams.get("vnp_PayDate");
        String vnpAmount = queryParams.get("vnp_Amount");

        String orderIdText = orderInfo != null ? orderInfo : vnpTxnRef;

        Map<String, Object> response = new HashMap<>();

        try {
            Long orderId = Long.parseLong(orderIdText.replaceAll("[^0-9]", ""));

            OrderDTO updatedOrder;
            if ("00".equals(vnpResponseCode)) {
                updatedOrder = orderService.updatePaymentOrderStatus(orderId, "PAID");

                if (vnpTxnRef != null && !vnpTxnRef.isBlank()) {
                    boolean exists = paymentRepository
                            .existsByOrderOrderIdAndPaymentMethodIgnoreCaseAndTransactionReference(
                                    orderId,
                                    "VNPAY",
                                    vnpTxnRef
                            );

                    if (!exists) {
                        Order order = orderRepository.findById(orderId).orElse(null);
                        if (order != null) {
                            BigDecimal amount = null;
                            if (vnpAmount != null && !vnpAmount.isBlank()) {
                                try {
                                    // VNPay sends amount in smallest currency unit (VND * 100)
                                    long raw = Long.parseLong(vnpAmount.trim());
                                    amount = BigDecimal.valueOf(raw).divide(BigDecimal.valueOf(100));
                                } catch (Exception ignored) {
                                    amount = null;
                                }
                            }
                            if (amount == null) {
                                amount = order.getFinalPrice() != null
                                        ? order.getFinalPrice()
                                        : (order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);
                            }

                            LocalDateTime paidAt = LocalDateTime.now();
                            if (vnpPayDate != null && !vnpPayDate.isBlank()) {
                                try {
                                    // vnp_PayDate format: yyyyMMddHHmmss (GMT+7)
                                    paidAt = LocalDateTime.parse(
                                            vnpPayDate.trim(),
                                            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                    );
                                } catch (Exception ignored) {
                                    paidAt = LocalDateTime.now();
                                }
                            }

                            Payment payment = Payment.builder()
                                    .order(order)
                                    .paymentMethod("VNPAY")
                                    .amount(amount)
                                    .status("SUCCESS")
                                    .transactionReference(vnpTxnRef)
                                    .paidAt(paidAt)
                                    .build();

                            paymentRepository.save(payment);
                        }
                    } else {
                        // Ensure existing payment has correct paidAt/amount so refunds can succeed.
                        Payment existingPayment = paymentRepository
                                .findFirstByOrderOrderIdAndPaymentMethodIgnoreCaseAndTransactionReference(
                                        orderId,
                                        "VNPAY",
                                        vnpTxnRef
                                );

                        if (existingPayment != null) {
                            boolean changed = false;

                            if (vnpAmount != null && !vnpAmount.isBlank()) {
                                try {
                                    long raw = Long.parseLong(vnpAmount.trim());
                                    BigDecimal amount = BigDecimal.valueOf(raw).divide(BigDecimal.valueOf(100));
                                    if (existingPayment.getAmount() == null || existingPayment.getAmount().compareTo(amount) != 0) {
                                        existingPayment.setAmount(amount);
                                        changed = true;
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            if (vnpPayDate != null && !vnpPayDate.isBlank()) {
                                try {
                                    LocalDateTime paidAt = LocalDateTime.parse(
                                            vnpPayDate.trim(),
                                            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                    );
                                    if (existingPayment.getPaidAt() == null || !existingPayment.getPaidAt().equals(paidAt)) {
                                        existingPayment.setPaidAt(paidAt);
                                        changed = true;
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            if (changed) {
                                paymentRepository.save(existingPayment);
                            }
                        }
                    }
                }

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