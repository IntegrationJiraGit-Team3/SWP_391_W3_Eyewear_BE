package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayRefundResult {
    private boolean success;
    private String refundStatus;
    private String responseCode;
    private String message;
    private String requestId;
    private String transactionReference;
    private String rawResponse;
}