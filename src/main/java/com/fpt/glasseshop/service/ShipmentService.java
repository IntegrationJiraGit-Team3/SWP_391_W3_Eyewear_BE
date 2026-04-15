package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.dto.CreateShipmentRequest;
import com.fpt.glasseshop.entity.dto.ShipmentDTO;
import com.fpt.glasseshop.entity.dto.UpdateShipmentStatusRequest;

public interface ShipmentService {
    ShipmentDTO createShipment(CreateShipmentRequest request);

    ShipmentDTO getByOrderId(Long orderId);

    ShipmentDTO getByTrackingNumber(String trackingNumber);

    ShipmentDTO updateShipment(Long shipmentId, UpdateShipmentStatusRequest request);
}