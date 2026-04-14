package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.OrderItemServiceRequest;
import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.SubmitServiceRequest;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.OrderItemServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@Tag(name = "Service Request API", description = "Submit and manage RETURN, WARRANTY, or REFUND requests on order items")
public class OrderItemServiceRequestController {

    private final OrderItemServiceRequestService service;
    private final UserAccountRepository userAccountRepository;

    // ✅ Lấy user từ JWT (SecurityContext)
    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    @PostMapping
    @Operation(summary = "Submit a service request", description = "Submit a return, warranty, or refund request for an order item.")
    public ResponseEntity<ApiResponse<OrderItemServiceRequest>> submitRequest(
            @Valid @RequestBody SubmitServiceRequest req) {

        try {
            UserAccount user = getCurrentUser();

            OrderItemServiceRequest savedRequest = service.submitRequest(user, req);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Service request submitted successfully", savedRequest));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/order-item/{orderItemId}")
    @Operation(summary = "Get requests by Order Item ID", description = "Retrieves a list of service requests associated with a specific order item.")
    public ResponseEntity<ApiResponse<List<OrderItemServiceRequest>>> getRequestsByOrderItemId(
            @PathVariable Long orderItemId) {

        List<OrderItemServiceRequest> requests = service.getRequestsByOrderItemId(orderItemId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @GetMapping
    @Operation(summary = "Get all requests", description = "Retrieves all service requests (For managers/admins).")
    public ResponseEntity<ApiResponse<List<OrderItemServiceRequest>>>
    getAllRequests() {

        List<OrderItemServiceRequest> requests = service.getAllRequests();
        return ResponseEntity.ok(ApiResponse.success(requests));
    }
}