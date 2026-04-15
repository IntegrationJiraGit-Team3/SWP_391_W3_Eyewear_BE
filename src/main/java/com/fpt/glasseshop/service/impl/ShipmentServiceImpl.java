package com.fpt.glasseshop.service.impl;

import com.fpt.glasseshop.entity.Order;
import com.fpt.glasseshop.entity.Shipment;
import com.fpt.glasseshop.entity.dto.CreateShipmentRequest;
import com.fpt.glasseshop.entity.dto.ShipmentDTO;
import com.fpt.glasseshop.entity.dto.UpdateShipmentStatusRequest;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.OrderRepository;
import com.fpt.glasseshop.repository.ShipmentRepository;
import com.fpt.glasseshop.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public ShipmentDTO createShipment(CreateShipmentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        Shipment shipment = shipmentRepository.findByOrderOrderId(order.getOrderId())
                .orElse(Shipment.builder().order(order).build());

        shipment.setCarrier(request.getCarrier());
        shipment.setTrackingNumber(
                request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()
                        ? request.getTrackingNumber()
                        : "TRK-" + System.currentTimeMillis()
        );
        shipment.setStatus(request.getStatus() != null ? request.getStatus() : "CREATED");
        shipment.setShippedDate(LocalDate.now());

        Shipment saved = shipmentRepository.save(shipment);

        if ("PENDING".equalsIgnoreCase(order.getStatus()) || "PROCESSING".equalsIgnoreCase(order.getStatus())) {
            order.setStatus("SHIPPED");
            orderRepository.save(order);
        }

        return toDTO(saved);
    }

    @Override
    public ShipmentDTO getByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found for order id: " + orderId));
        return toDTO(shipment);
    }

    @Override
    public ShipmentDTO getByTrackingNumber(String trackingNumber) {
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with tracking number: " + trackingNumber));
        return toDTO(shipment);
    }

    @Override
    @Transactional
    public ShipmentDTO updateShipment(Long shipmentId, UpdateShipmentStatusRequest request) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with id: " + shipmentId));

        if (request.getCarrier() != null) shipment.setCarrier(request.getCarrier());
        if (request.getTrackingNumber() != null) shipment.setTrackingNumber(request.getTrackingNumber());
        if (request.getStatus() != null) shipment.setStatus(request.getStatus());

        if ("DELIVERED".equalsIgnoreCase(shipment.getStatus())) {
            shipment.setDeliveredDate(LocalDate.now());
            Order order = shipment.getOrder();
            order.setStatus("DELIVERED");
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus("PAID");
            }
            orderRepository.save(order);
        }

        return toDTO(shipmentRepository.save(shipment));
    }

    private ShipmentDTO toDTO(Shipment shipment) {
        return ShipmentDTO.builder()
                .shipmentId(shipment.getShipmentId())
                .orderId(shipment.getOrder() != null ? shipment.getOrder().getOrderId() : null)
                .orderCode(shipment.getOrder() != null ? shipment.getOrder().getOrderCode() : null)
                .carrier(shipment.getCarrier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedDate(shipment.getShippedDate())
                .deliveredDate(shipment.getDeliveredDate())
                .status(shipment.getStatus())
                .build();
    }
}