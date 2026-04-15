package com.fpt.glasseshop.entity.dto;

import lombok.Data;

@Data
public class CreateShipmentRequest {
    private Long orderId;
    private String carrier;
    private String trackingNumber;
    private String status;
}