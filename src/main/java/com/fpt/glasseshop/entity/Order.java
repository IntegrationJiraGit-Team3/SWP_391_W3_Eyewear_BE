package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(unique = true, nullable = false, updatable = false)
    private String orderCode;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @CreationTimestamp
    private LocalDateTime orderDate;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    private String status;
    private BigDecimal totalPrice;

    @ManyToOne
    @JoinColumn(name = "shipping_address_id")
    private Address shippingAddress;

    @ManyToOne
    @JoinColumn(name = "billing_address_id")
    private Address billingAddress;

    private String paymentStatus;
    private String paymentMethod;

    @Column(columnDefinition = "nvarchar(255)")
    private String fullName;
    private String phone;
    @Column(columnDefinition = "nvarchar(255)")
    private String address;
    @Column(columnDefinition = "nvarchar(255)")
    private String note;
    
    private BigDecimal shippingFee;
    private BigDecimal voucherDiscount;
    private BigDecimal finalPrice;

    private BigDecimal depositAmount;
    private String depositType; // FULL or PARTIAL
    private String depositPaymentMethod; // Method used for the initial deposit
    private LocalDateTime stockReadyAt; // Timestamp when stock became available (preorders)

    @Column(unique = true)
    private String idempotencyKey;

    @Column(name = "refund_status")
    private String refundStatus;

    @Column(name = "refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @Column(name = "refund_processed_at")
    private LocalDateTime refundProcessedAt;

    @Column(name = "refund_bank_account_number", columnDefinition = "NVARCHAR(50)")
    private String refundBankAccountNumber;

    @Column(name = "refund_bank_name", columnDefinition = "NVARCHAR(100)")
    private String refundBankName;

    @Column(name = "refund_bank_account_holder", columnDefinition = "NVARCHAR(100)")
    private String refundBankAccountHolder;

    @Column(name = "refund_note", columnDefinition = "NVARCHAR(MAX)")
    private String refundNote;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;

    @PrePersist
    public void prePersist() {
        if (this.orderCode == null) {
            this.orderCode = "ORD-" + java.util.UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        }
    }

}
