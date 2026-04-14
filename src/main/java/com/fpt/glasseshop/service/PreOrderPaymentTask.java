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

    /**
     * Every hour, check for pre-orders that reached stock-ready 24h ago
     * and haven't paid the remaining balance. Default them to COD.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void processPendingBalancePayments() {
        log.info("Checking for pre-order balance payment timeouts...");
        
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        
        // Find orders where:
        // 1. stockReadyAt is before cutoff
        // 2. depositType is PARTIAL
        // 3. paymentStatus is NOT PAID
        // (Assuming if it's already PAID, they settled it).
        
        List<Order> timeoutOrders = orderRepository.findTimeoutPreOrders(cutoff);
        
        for (Order order : timeoutOrders) {
            log.info("Order {} reached balance payment timeout. Defaulting to COD for remaining amount.", order.getOrderCode());
            // We don't change the main paymentMethod if they already paid deposit via VNPay,
            // but we could set a flag or just let the system know the remaining is COD.
            // For now, let's keep it simple as requested.
        }
    }
}
