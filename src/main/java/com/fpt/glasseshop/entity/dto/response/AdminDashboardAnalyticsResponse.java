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
    private BigDecimal grossRevenue;
    private BigDecimal netRevenue;
    private BigDecimal pendingRevenue;
    private BigDecimal projectedRevenue;
    private BigDecimal refundedAmount;
    private BigDecimal remainingRevenueAfterRefund;

    private BigDecimal totalFrameRevenue;
    private BigDecimal totalLensRevenue;
    private BigDecimal totalProductRevenue;

    private BigDecimal codRevenue;
    private BigDecimal vnpayRevenue;

    private Long totalCustomers;
    private Long newCustomers;
    private Long soldItems;

    private Long totalOrders;
    private Long completedOrders;
    private Long pendingOrders;
    private Long processingOrders;
    private Long shippingOrders;
    private Long cancelledOrders;

    private Long refundedOrders;
    private Long processedOrders;

    private Long totalReturnRequests;
    private Long pendingReturnRequests;
    private Long refundPendingCount;
    private Long refundedCount;

    private Double revenueChangePercent;
    private Double customerChangePercent;
    private Double soldItemsChangePercent;
    private Double refundedChangePercent;
    private Double refundRate;
    private Double completionRate;

    private List<OrderStatusReportResponse> orderStatusReport;
    private List<ReturnStatusReportResponse> returnStatusReport;
    private List<DashboardTimePointResponse> timeline;
    private List<ProductReportResponse> bestSellingProductsByQuantity;
    private List<ProductReportResponse> bestSellingProductsByRevenue;

    private List<ProductReportResponse> bestSellingFramesByQuantity;
    private List<ProductReportResponse> bestSellingFramesByRevenue;

    private List<LensReportResponse> bestSellingLensesByQuantity;
    private List<LensReportResponse> bestSellingLensesByRevenue;

    private List<LensReportResponse> lensSalesReport;
    private List<ProductInventoryReportResponse> productInventoryReport;
    private List<CustomerPurchaseSummaryResponse> topCustomersBySpending;
    private List<CustomerInsightResponse> customerInsights;

    private List<OrderInsightResponse> topOrders;
    private List<OrderInsightResponse> cancelledOrdersReport;
}