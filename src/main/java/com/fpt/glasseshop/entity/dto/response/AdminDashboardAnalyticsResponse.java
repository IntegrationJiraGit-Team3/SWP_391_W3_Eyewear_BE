package com.fpt.glasseshop.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardAnalyticsResponse {
    private LocalDate fromDate;
    private LocalDate toDate;
    private String groupBy;

    private BigDecimal totalRevenue;
    private BigDecimal projectedRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal remainingRevenueAfterRefund;

    private Long totalCustomers;
    private Long newCustomers;
    private Long soldItems;

    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippingOrders;
    private Long cancelledOrders;

    private Double revenueChangePercent;
    private Double customerChangePercent;
    private Double soldItemsChangePercent;
    private Double refundedChangePercent;

    private List<DashboardTimePointResponse> timeline;
    private List<OrderStatusReportResponse> orderStatusReport;
    private List<ProductReportResponse> bestSellingProductsByQuantity;
    private List<ProductReportResponse> bestSellingProductsByRevenue;
}