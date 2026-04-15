package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.CreateShipmentRequest;
import com.fpt.glasseshop.entity.dto.ShipmentDTO;
import com.fpt.glasseshop.entity.dto.UpdateShipmentStatusRequest;
import com.fpt.glasseshop.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentDTO>> createShipment(@RequestBody CreateShipmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Shipment created successfully", shipmentService.createShipment(request)));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ShipmentDTO>> getShipmentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("Shipment loaded successfully", shipmentService.getByOrderId(orderId)));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<ShipmentDTO>> getShipmentByTracking(@PathVariable String trackingNumber) {
        return ResponseEntity.ok(ApiResponse.success("Shipment loaded successfully", shipmentService.getByTrackingNumber(trackingNumber)));
    }

    @PatchMapping("/{shipmentId}")
    public ResponseEntity<ApiResponse<ShipmentDTO>> updateShipment(
            @PathVariable Long shipmentId,
            @RequestBody UpdateShipmentStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Shipment updated successfully", shipmentService.updateShipment(shipmentId, request)));
    }
}