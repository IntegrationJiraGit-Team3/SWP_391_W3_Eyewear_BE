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

    List<Order> findAllByOrderByOrderDateDesc();

    @Query("""
            SELECT COALESCE(SUM(o.finalPrice), 0)
            FROM Order o
            WHERE o.status IN ('DELIVERED', 'COMPLETED')
           """)
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
           """)
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
            WHERE o.paymentStatus IN ('PAID', 'PAID_FULL')
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
            WHERE o.paymentStatus IN ('PAID', 'PAID_FULL')
           """)
    long countCustomersPaid();

    long countByPaymentStatus(String paymentStatus);

    @Query("""
        SELECT o FROM Order o
        WHERE o.depositType = 'PARTIAL'
          AND o.paymentStatus <> 'PAID'
          AND o.stockReadyAt <= :cutoff
          AND o.status NOT IN ('CANCELLED', 'CANCELED')
    """)
    List<Order> findTimeoutPreOrders(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
        SELECT o FROM Order o
        WHERE o.paymentMethod = 'VNPAY'
          AND o.paymentStatus = 'UNPAID'
          AND o.status IN ('PENDING', 'PREORDER')
          AND o.orderDate <= :cutoff
    """)
    List<Order> findExpiredPendingVnpayOrders(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
        SELECT COALESCE(SUM(o.finalPrice), 0)
        FROM Order o
        WHERE o.paymentStatus IN ('PAID', 'PAID_FULL')
    """)
    BigDecimal calculateCollectedCash();

    @Query("""
        SELECT COALESCE(SUM(o.finalPrice), 0)
        FROM Order o
        WHERE o.status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERING', 'PREORDER')
          AND o.paymentStatus IN ('PAID', 'PAID_FULL')
    """)
    BigDecimal calculateCurrentHeldMoney();

    @Query("""
        SELECT COUNT(o)
        FROM Order o
        WHERE o.status = :status
    """)
    Long countByOrderStatus(@Param("status") String status);

    @Query("""
        SELECT COALESCE(SUM(o.finalPrice), 0)
        FROM Order o
        WHERE o.status IN ('DELIVERED', 'COMPLETED')
          AND o.orderDate >= :from
          AND o.orderDate <= :to
    """)
    BigDecimal calculateGrossRevenueBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );


    @Query("""
        SELECT o FROM Order o
        WHERE o.depositType = 'PARTIAL'
          AND o.remainingPaymentStatus = 'UNPAID'
          AND o.remainingPaymentDueAt IS NOT NULL
          AND o.remainingPaymentDueAt <= :cutoff
          AND o.status NOT IN ('CANCELLED', 'CANCELED', 'DELIVERED', 'COMPLETED', 'REFUNDED')
    """)
    List<Order> findOrdersWithExpiredRemainingPayment(@Param("cutoff") LocalDateTime cutoff);

}