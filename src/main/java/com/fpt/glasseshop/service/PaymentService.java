package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.Payment;
import com.fpt.glasseshop.entity.dto.PaymentRequest;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Payment processPayment(PaymentRequest request, String userEmail) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        // Verification: Current user must be the owner of the order
        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You are not authorized to pay for this order");
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            throw new IllegalArgumentException("Order is already paid");
        }

        // Mock Payment Gateway Logic
        // In a real system, you would call Stripe/VNPay API here
        boolean isSuccess = !"fail".equalsIgnoreCase(request.getPaymentToken());

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(order.getTotalPrice())
                .paidAt(LocalDateTime.now())
                .transactionReference(UUID.randomUUID().toString())
                .build();

        if (isSuccess) {
            payment.setStatus("SUCCESS");
            order.setPaymentStatus("PAID");
            order.setStatus("PROCESSING"); // Move from PENDING to PROCESSING once paid
        } else {
            payment.setStatus("FAILED");
            order.setPaymentStatus("FAILED");
        }

        orderRepository.save(order);
        return paymentRepository.save(payment);
    }
}
