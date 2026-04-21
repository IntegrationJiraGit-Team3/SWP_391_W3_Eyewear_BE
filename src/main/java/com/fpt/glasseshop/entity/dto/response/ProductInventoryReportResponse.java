package com.fpt.glasseshop.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventoryReportResponse {
    private String productName;
    private String productType;
    private String color;
    private Long purchasedQuantity;
    private Long inStockQuantity;
    private Long preorderQuantity;

    private java.math.BigDecimal revenue;
    private java.math.BigDecimal completedRevenue;
    private java.math.BigDecimal pendingRevenue;
    private java.math.BigDecimal refundedRevenue;
}