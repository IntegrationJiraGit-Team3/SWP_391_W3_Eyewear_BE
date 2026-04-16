package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "return_request_transaction")
public class ReturnRequestTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_before", length = 50)
    private ReturnRequest.ReturnStatus statusBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_after", nullable = false, length = 50)
    private ReturnRequest.ReturnStatus statusAfter;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "transaction_reference", length = 100)
    private String transactionReference;

    @Column(name = "note", columnDefinition = "NVARCHAR(MAX)")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum Action {
        CREATED,
        APPROVED,
        REJECTED,
        RECEIVED_RETURN,
        REFUND_PENDING,
        REFUND_INFO_INVALID,
        REFUND_INFO_UPDATED,
        REFUNDED,
        CUSTOMER_CONFIRMED_REFUND_RECEIVED,
        COMPLETED
    }
}