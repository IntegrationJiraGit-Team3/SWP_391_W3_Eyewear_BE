package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.OrderItemServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemServiceRequestRepository extends JpaRepository<OrderItemServiceRequest, Long> {
    List<OrderItemServiceRequest> findByOrderItemOrderItemId(Long orderItemId);
}
