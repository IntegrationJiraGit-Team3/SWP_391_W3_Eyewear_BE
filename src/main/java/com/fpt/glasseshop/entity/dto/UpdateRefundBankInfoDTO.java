package com.fpt.glasseshop.entity.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UpdateRefundBankInfoDTO {
    private String bankAccountNumber;
    private String bankName;
    private String bankAccountHolder;
}
