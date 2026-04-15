package com.fpt.glasseshop.entity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ShipmentDTO {
    private Long shipmentId;
    private Long orderId;
    private String orderCode;
    private String carrier;
    private String trackingNumber;
    private LocalDate shippedDate;
    private LocalDate deliveredDate;
    private String status;
}