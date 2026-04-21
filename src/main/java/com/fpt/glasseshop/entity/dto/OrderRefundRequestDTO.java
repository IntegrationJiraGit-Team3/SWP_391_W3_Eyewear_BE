package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRefundRequestDTO {
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private String note;
}
