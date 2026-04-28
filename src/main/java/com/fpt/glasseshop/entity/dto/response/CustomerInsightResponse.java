package com.fpt.glasseshop.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerInsightResponse {
    private Long userId;
    private String customerName;

    private Long totalOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long refundedOrders;

    private BigDecimal totalSpent;
    private BigDecimal totalRefunded;
    private BigDecimal averageOrderValue;

    private String favoriteProductName;
    private Long favoriteProductQuantity;
}