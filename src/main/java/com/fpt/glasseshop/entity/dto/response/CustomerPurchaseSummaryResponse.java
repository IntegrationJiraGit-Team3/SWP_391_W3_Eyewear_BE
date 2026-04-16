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
public class CustomerPurchaseSummaryResponse {
    private Long userId;
    private String customerName;
    private Long orderCount;
    private BigDecimal totalSpent;
}
