package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderOrderId(Long orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE EXISTS (SELECT p FROM Prescription p WHERE p.orderItem = oi)")
    List<OrderItem> findAllPrescriptionItems();

    @Query("SELECT oi FROM OrderItem oi WHERE EXISTS (SELECT po FROM PreOrder po WHERE po.orderItem = oi) OR oi.isPreorder = true")
    List<OrderItem> findAllPreOrderItems();

    @Query("SELECT oi FROM OrderItem oi WHERE NOT EXISTS (SELECT po FROM PreOrder po WHERE po.orderItem = oi) AND (oi.isPreorder = false OR oi.isPreorder IS NULL) AND oi.prescription IS NULL")
    List<OrderItem> findAllInStockItems();

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE EXISTS (SELECT p FROM Prescription p WHERE p.orderItem = oi)")
    long countPrescriptionItems();

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE EXISTS (SELECT po FROM PreOrder po WHERE po.orderItem = oi) OR oi.isPreorder = true")
    long countPreOrderItems();

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE NOT EXISTS (SELECT po FROM PreOrder po WHERE po.orderItem = oi) AND (oi.isPreorder = false OR oi.isPreorder IS NULL) AND oi.prescription IS NULL")
    long countInStockItems();

    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.variant.product.productId = :productId")
    long countByProductId(Long productId);

}
