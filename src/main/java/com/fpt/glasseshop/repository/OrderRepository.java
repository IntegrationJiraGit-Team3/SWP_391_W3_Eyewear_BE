package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserUserId(Long userId);
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT COALESCE(SUM(o.finalPrice), 0)
            FROM Order o 
            WHERE o.status IN ('DELIVERED', 'COMPLETED')
           """ )
    BigDecimal calculateTotalRevenue();

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.status IN ('DELIVERED', 'COMPLETED')
           """)
    Long countDeliveredOrders();

    @Query("""
            SELECT COALESCE(SUM(o.finalPrice), 0)
            FROM Order o 
            WHERE o.status IN ('DELIVERED', 'COMPLETED')
            AND o.orderDate >= :from
            AND o.orderDate <= :to           
           """ )
    BigDecimal calculateRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.status IN ('DELIVERED', 'COMPLETED')
            AND o.orderDate >= :from
            AND o.orderDate <= :to
    """)
    Long countDeliveredOrdersBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.paymentStatus = 'PAID'
        AND o.orderDate >= :from
        AND o.orderDate <= :to
    """)
    Long countPaidOrdersBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
    SELECT COUNT(DISTINCT o.user.userId)
        FROM Order o
        WHERE o.paymentStatus = 'PAID'
    """)
    long countCustomersPaid();

    long countByPaymentStatus(String paymentStatus);

    @Query("""
        SELECT o FROM Order o 
        WHERE o.depositType = 'PARTIAL' 
        AND o.paymentStatus != 'PAID' 
        AND o.stockReadyAt <= :cutoff
        AND o.status NOT IN ('CANCELLED', 'CANCELED')
    """)
    List<Order> findTimeoutPreOrders(@Param("cutoff") LocalDateTime cutoff);

    List<Order> findAllByOrderByOrderDateDesc();
}
