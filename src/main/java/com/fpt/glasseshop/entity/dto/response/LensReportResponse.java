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
public class LensReportResponse {
    private String lensType;
    private Long quantitySold;
    private Long cancelledQuantity;
    private BigDecimal revenue;
    private BigDecimal cancelledRevenue;
    private BigDecimal completedRevenue;
    private BigDecimal pendingRevenue;
    private BigDecimal refundedRevenue;
}
