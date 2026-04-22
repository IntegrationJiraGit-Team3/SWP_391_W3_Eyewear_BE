package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {
    private String bankAccountNumber;
    private String bankName;
    private String bankAccountHolder;
    private String note;
}
