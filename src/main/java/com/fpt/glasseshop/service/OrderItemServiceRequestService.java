package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.OrderItem;
import com.fpt.glasseshop.entity.OrderItemServiceRequest;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.SubmitServiceRequest;
import com.fpt.glasseshop.repository.OrderItemRepository;
import com.fpt.glasseshop.repository.OrderItemServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderItemServiceRequestService {

    private final OrderItemServiceRequestRepository serviceRequestRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public OrderItemServiceRequest submitRequest(UserAccount user, SubmitServiceRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new IllegalArgumentException("OrderItem not found"));

        if (!orderItem.getOrder().getUser().getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("You are not authorized to submit a request for this item");
        }

        OrderItemServiceRequest serviceRequest = OrderItemServiceRequest.builder()
                .orderItem(orderItem)
                .requestType(request.getRequestType())
                .reason(request.getReason())
                .status(OrderItemServiceRequest.RequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        return serviceRequestRepository.save(serviceRequest);
    }

    public List<OrderItemServiceRequest> getRequestsByOrderItemId(Long orderItemId) {
        return serviceRequestRepository.findByOrderItemOrderItemId(orderItemId);
    }

    public List<OrderItemServiceRequest> getAllRequests() {
        return serviceRequestRepository.findAll();
    }
}
