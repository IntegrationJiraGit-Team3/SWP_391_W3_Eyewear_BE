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

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipmentServiceImpl implements ShipmentService {

    private static final Map<String, Integer> CITY_DISTANCE_MAP = Map.ofEntries(
            Map.entry("ho chi minh", 1),
            Map.entry("hcm", 1),
            Map.entry("tp hcm", 1),
            Map.entry("tphcm", 1),
            Map.entry("ho chi minh city", 1),
            Map.entry("binh duong", 5),
            Map.entry("dong nai", 6),
            Map.entry("ba ria vung tau", 7),
            Map.entry("long an", 8),
            Map.entry("tien giang", 9),
            Map.entry("vinh long", 10),
            Map.entry("can tho", 12),
            Map.entry("an giang", 13),
            Map.entry("soc trang", 14),
            Map.entry("bac lieu", 15),
            Map.entry("ca mau", 16),
            Map.entry("kien giang", 18),
            Map.entry("tra vinh", 11)
    );

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

        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            shipment.setTrackingNumber(request.getTrackingNumber());
        } else if (shipment.getTrackingNumber() == null || shipment.getTrackingNumber().isBlank()) {
            shipment.setTrackingNumber("TRK-" + System.currentTimeMillis());
        }

        shipment.setStatus(request.getStatus() != null ? request.getStatus() : "CREATED");
        shipment.setShippedDate(LocalDate.now());
        shipment.setDeliveredDate(calculateEstimatedDeliveredDate(order, shipment.getShippedDate()));

        return toDTO(shipmentRepository.save(shipment));
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
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank()) {
            shipment.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getStatus() != null) shipment.setStatus(request.getStatus());

        Order order = shipment.getOrder();
        String shipmentStatus = shipment.getStatus() != null ? shipment.getStatus().trim().toUpperCase() : "";

        if ("DELIVERED".equalsIgnoreCase(shipmentStatus)) {
            shipment.setDeliveredDate(LocalDate.now());

            String oldOrderStatus = order.getStatus();
            boolean isPreorderPartial =
                    "PARTIAL".equalsIgnoreCase(order.getDepositType())
                            || "PREORDER".equalsIgnoreCase(oldOrderStatus);

            order.setStatus("DELIVERED");

            if (!isPreorderPartial && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus("PAID");
            }

            orderRepository.save(order);
        } else {
            if (shipment.getShippedDate() == null) {
                shipment.setShippedDate(LocalDate.now());
            }
            shipment.setDeliveredDate(calculateEstimatedDeliveredDate(order, shipment.getShippedDate()));
        }

        return toDTO(shipmentRepository.save(shipment));
    }

    private LocalDate calculateEstimatedDeliveredDate(Order order, LocalDate shippedDate) {
        LocalDate baseDate = shippedDate != null ? shippedDate : LocalDate.now();
        return baseDate.plusDays(resolveTransitDays(order));
    }

    private long resolveTransitDays(Order order) {
        Integer distance = extractDistance(order);
        if (distance != null) {
            return distance < 10 ? 3 : 5;
        }

        BigDecimal shippingFee = order.getShippingFee();
        if (shippingFee != null && shippingFee.compareTo(BigDecimal.ZERO) > 0) {
            return shippingFee.compareTo(BigDecimal.valueOf(100000)) < 0 ? 3 : 5;
        }

        return 3;
    }

    private Integer extractDistance(Order order) {
        String city = null;

        if (order.getShippingAddress() != null && order.getShippingAddress().getCity() != null) {
            city = order.getShippingAddress().getCity();
        } else if (order.getAddress() != null && order.getAddress().contains(",")) {
            String[] parts = order.getAddress().split(",");
            city = parts[parts.length - 1];
        }

        if (city == null || city.isBlank()) {
            return null;
        }

        return CITY_DISTANCE_MAP.get(normalizeLocation(city));
    }

    private String normalizeLocation(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replace("?", "d")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.startsWith("tp ")) {
            normalized = normalized.substring(3).trim();
        }

        return normalized;
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


