package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreOrderPaymentTask {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void handleExpiredPreorderPayments() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderRepository.findOrdersWithExpiredRemainingPayment(now);

        for (Order order : orders) {
            if (!"PARTIAL".equalsIgnoreCase(order.getDepositType())) continue;
            if (!"UNPAID".equalsIgnoreCase(order.getRemainingPaymentStatus())) continue;

            order.setRemainingPaymentStatus("COD");
            order.setRemainingPaymentDueAt(null);
            orderRepository.save(order);

            log.info("Preorder {} switched remaining payment to COD", order.getOrderCode());
        }
    }
}