package com.fpt.glasseshop.entity.dto.response;

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
public class OrderInsightResponse {
    private Long orderId;
    private String orderCode;
    private String customerName;
    private String status;
    private BigDecimal totalAmount;
    private Long itemCount;
    private LocalDateTime orderDate;
}