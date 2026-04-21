package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrderOrderIdAndPaymentMethodIgnoreCaseAndTransactionReference(
	    Long orderId,
	    String paymentMethod,
	    String transactionReference
    );

    Payment findFirstByOrderOrderIdAndPaymentMethodIgnoreCaseAndTransactionReference(
	    Long orderId,
	    String paymentMethod,
	    String transactionReference
    );

    Payment findTopByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountGreaterThanAndStatusOrderByPaidAtDesc(
	    Long orderId,
	    String paymentMethod,
	    BigDecimal minAmount,
	    String status
    );

    boolean existsByOrderOrderIdAndPaymentMethodIgnoreCaseAndAmountLessThan(
	    Long orderId,
	    String paymentMethod,
	    BigDecimal maxAmount
    );

    @Query("""
	    SELECT COALESCE(SUM(p.amount), 0)
	    FROM Payment p
	    WHERE p.amount < 0
	      AND UPPER(p.paymentMethod) = 'VNPAY'
	      AND p.paidAt >= :from
	      AND p.paidAt <= :to
	    """)
    BigDecimal sumVnpayRefundedAmountsBetween(
	    @Param("from") LocalDateTime from,
	    @Param("to") LocalDateTime to
    );
}
