package com.fpt.glasseshop.entity.dto;

import lombok.Data;

@Data
public class UpdateShipmentStatusRequest {
    private String carrier;
    private String trackingNumber;
    private String status;
}