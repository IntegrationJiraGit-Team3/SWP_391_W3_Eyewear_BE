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
    private String color;
    private Long purchasedQuantity;
    private Long inStockQuantity;
    private Long preorderQuantity;
}