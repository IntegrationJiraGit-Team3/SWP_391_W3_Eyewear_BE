package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long countByProductId(Long productId);

    List<OrderItem> findByOrderOrderId(Long orderId);
}