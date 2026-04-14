package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private String fullName;
    private String phone;
    private String address;
    private String note;
    private String paymentMethod;
    private java.math.BigDecimal shippingFee;
    private java.math.BigDecimal voucherDiscount;
    private String idempotencyKey;
    private Boolean isPreorder;
    private String depositType;
    private String shipmentStatus;
}
