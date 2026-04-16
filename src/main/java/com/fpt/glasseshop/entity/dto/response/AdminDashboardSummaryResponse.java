package com.fpt.glasseshop.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardSummaryResponse {
    private LocalDate fromDate;
    private LocalDate toDate;

    private BigDecimal grossRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal netRevenue;
    private BigDecimal remainingRevenueAfterRefund;

    private BigDecimal collectedCash;
    private BigDecimal currentHeldMoney;

    private Long totalOrders;
    private Long deliveredOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippingOrders;
    private Long cancelledOrders;

    private Long refundPendingCount;
    private Long refundedCount;

    private LocalDateTime timestamp;
}