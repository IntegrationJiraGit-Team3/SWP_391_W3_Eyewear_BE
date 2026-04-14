package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItem saveOrderItem(OrderItem item) {
        return orderItemRepository.save(item);
    }

    public Optional<OrderItem> getOrderItemById(Long orderItemId) {

        return orderItemRepository.findById(orderItemId);
    }

    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderOrderId(orderId);
    }

    public OrderItem updateFulfillmentType(Long orderItemId, String fulfillmentType) {
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found with id: " + orderItemId));
        item.setFulfillmentType(fulfillmentType);
        return orderItemRepository.save(item);
    }

    public void deleteOrderItem(Long orderItemId) {
        orderItemRepository.deleteById(orderItemId);
    }

    public List<OrderItem> getPrescriptionItems() {
        return orderItemRepository.findAllPrescriptionItems();
    }

    public List<OrderItem> getPreOrderItems() {
        return orderItemRepository.findAllPreOrderItems();
    }

    public List<OrderItem> getInStockItems() {
        return orderItemRepository.findAllInStockItems();
    }

    public long countPrescriptionItems() {
        return orderItemRepository.countPrescriptionItems();
    }

    public long countPreOrderItems() {
        return orderItemRepository.countPreOrderItems();
    }

    public long countInStockItems() {
        return orderItemRepository.countInStockItems();
    }

    public java.util.Map<String, Long> getOrderItemStats() {
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        stats.put("PRESCRIPTION", countPrescriptionItems());
        stats.put("PRE_ORDER", countPreOrderItems());
        stats.put("IN_STOCK", countInStockItems());
        return stats;
    }
}
