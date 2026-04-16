package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundProcessDTO {
    private String paymentMethod;
    private String transactionReference;
    private String note;
}