package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderOrderId(Long orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}