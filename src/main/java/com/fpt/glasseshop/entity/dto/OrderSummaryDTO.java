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
public class OrderSummaryDTO {
    private Long orderId;
    private LocalDateTime orderDate;
    private String status;
    private BigDecimal totalPrice;
    private Integer totalItems;
    private String paymentStatus;
}
