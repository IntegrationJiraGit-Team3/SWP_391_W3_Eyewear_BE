package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestDTO {
    private Long orderId;
    private Long orderItemId;
    private String reason;
    private String description;
    private String imageUrl;
    private String requestType;
    private Integer returnQuantity;
    private String bankAccountNumber;
    private String bankName;
    private String bankAccountHolder;
    private Boolean isComboRequest;
}
