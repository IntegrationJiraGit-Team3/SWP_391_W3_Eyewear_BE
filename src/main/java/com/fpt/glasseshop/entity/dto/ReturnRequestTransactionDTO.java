package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestTransactionDTO {
    private Long transactionId;
    private String action;
    private String statusBefore;
    private String statusAfter;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionReference;
    private String note;
    private LocalDateTime createdAt;
}