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
public class DashboardTimePointResponse {
    private String label;
    private BigDecimal revenue;
    private Long customerRegistrations;
    private Long soldItems;
    private BigDecimal refundedAmount;
}