package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestResponseDTO {
    private Long requestId;
    private Long orderId;
    private Long orderItemId;
    private String reason;
    private String description;
    private String imageUrl;
    private String status;
    private String rejectionReason;
    private LocalDateTime requestedAt;
    private LocalDateTime resolvedAt;
    private String requestType;
    private Long replacementOrderId;
    private Integer returnQuantity;
    private Long replacementOrderItemId;

    private String productName;
    private String productImageUrl;
    private String variantColor;
    private String variantSize;
    private Integer purchasedQuantity;
    private BigDecimal unitPrice;

    private String lensType;
    private String lensCoating;

    private String bankAccountNumber;
    private String bankName;
    private String bankAccountHolder;
    private String refundNote;

    private BigDecimal refundAmount;
    private List<ReturnRequestTransactionDTO> transactions;
}