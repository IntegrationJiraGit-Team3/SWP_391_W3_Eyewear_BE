package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.UserAccount;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.CreateOrderRequest;
import com.fpt.glasseshop.entity.dto.OrderDTO;
import com.fpt.glasseshop.entity.dto.OrderRefundRequestDTO;
import com.fpt.glasseshop.entity.dto.RefundProcessDTO;
import com.fpt.glasseshop.entity.dto.VNPayRefundResult;
import com.fpt.glasseshop.exception.ResourceNotFoundException;
import com.fpt.glasseshop.repository.UserAccountRepository;
import com.fpt.glasseshop.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "OrderAPI", description = "Operations related to orders")
public class OrderRestController {

    private final OrderService orderService;
    private final UserAccountRepository userAccountRepository;

    private UserAccount getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        if (email == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private void checkIfOwnerOrStaff(Long userId, UserAccount currentUser) {
        boolean isStaffOrAdmin = "ADMIN".equals(currentUser.getRole())
                || "OPERATIONAL_STAFF".equals(currentUser.getRole());

        if (!userId.equals(currentUser.getUserId()) && !isStaffOrAdmin) {
            throw new AccessDeniedException("You are not authorized to access this information");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getAllOrders() {
        List<OrderDTO> orders = orderService.getAllOrdersDTO();
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long id) {
        return orderService.getOrderDTOById(id)
                .map(order -> {
                    UserAccount currentUser = getCurrentUser();
                    checkIfOwnerOrStaff(order.getUserId(), currentUser);
                    return ResponseEntity.ok(ApiResponse.success(order));
                })
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("Order not found")));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrdersByUserId(@PathVariable Long userId) {
        UserAccount currentUser = getCurrentUser();
        checkIfOwnerOrStaff(userId, currentUser);

        List<OrderDTO> orders = orderService.getOrdersDTOByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(@Valid @RequestBody CreateOrderRequest req) {
        try {
            UserAccount user = getCurrentUser();
            OrderDTO orderDTO = orderService.createOrderFromCart(user, req);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Order completed successfully", orderDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Error during checkout: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {
            OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Order status updated", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/paymentStatus")
    public ResponseEntity<ApiResponse<OrderDTO>> paymentOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {
            OrderDTO updatedOrder = orderService.updatePaymentOrderStatus(id, status);
            return ResponseEntity.ok(ApiResponse.success("Order payment status updated", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/paymentMethod")
    public ResponseEntity<ApiResponse<OrderDTO>> updatePaymentMethod(
            @PathVariable Long id,
            @RequestParam String method) {
        try {
            OrderDTO updatedOrder = orderService.updatePaymentMethod(id, method);
            return ResponseEntity.ok(ApiResponse.success("Payment method updated", updatedOrder));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/cancel-payment")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelPendingPayment(@PathVariable Long id) {
        try {
            OrderDTO updatedOrder = orderService.cancelPendingPayment(id);
            return ResponseEntity.ok(ApiResponse.success("Pending payment cancelled", updatedOrder));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/refund-vnpay")
    public ResponseEntity<ApiResponse<VNPayRefundResult>> refundVnpay(
            @PathVariable Long id,
            @RequestParam(required = false) String reason
    ) {
        try {
            UserAccount currentUser = getCurrentUser();

            boolean isStaffOrAdmin = "ADMIN".equals(currentUser.getRole())
                    || "OPERATIONAL_STAFF".equals(currentUser.getRole());

            if (!isStaffOrAdmin) {
                throw new AccessDeniedException("You are not authorized to process VNPay refunds");
            }

            VNPayRefundResult result = orderService.refundVnpayForCancelledOrder(id, currentUser.getEmail(), reason);
            return ResponseEntity.ok(ApiResponse.success("VNPay refund processed", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Refund failed: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/request-refund")
    public ResponseEntity<ApiResponse<OrderDTO>> requestRefund(
            @PathVariable Long id,
            @RequestBody OrderRefundRequestDTO dto
    ) {
        try {
            UserAccount currentUser = getCurrentUser();
            OrderDTO updatedOrder = orderService.requestRefundForCancelledOrder(id, currentUser.getEmail(), dto);
            return ResponseEntity.ok(ApiResponse.success("Refund request submitted", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Refund request failed: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/request-vnpay-refund")
    public ResponseEntity<ApiResponse<OrderDTO>> requestVnpayRefund(
            @PathVariable Long id,
            @RequestBody(required = false) RefundProcessDTO dto
    ) {
        try {
            UserAccount currentUser = getCurrentUser();
            String reason = dto != null ? dto.getNote() : null;
            OrderDTO updatedOrder = orderService.requestVnpayRefundForCancelledOrder(id, currentUser.getEmail(), reason);
            return ResponseEntity.ok(ApiResponse.success("Refund request submitted", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Refund request failed: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/confirm-refunded")
    public ResponseEntity<ApiResponse<OrderDTO>> confirmRefunded(
            @PathVariable Long id,
            @RequestBody(required = false) RefundProcessDTO dto
    ) {
        try {
            UserAccount currentUser = getCurrentUser();
            boolean isStaffOrAdmin = "ADMIN".equals(currentUser.getRole())
                    || "OPERATIONAL_STAFF".equals(currentUser.getRole());

            if (!isStaffOrAdmin) {
                throw new AccessDeniedException("You are not authorized to confirm refunds");
            }

            OrderDTO updatedOrder = orderService.confirmRefundedForCancelledOrder(id, currentUser.getEmail(), dto);
            return ResponseEntity.ok(ApiResponse.success("Refund confirmed", updatedOrder));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Confirm refund failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable Long id) {
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/customers/count")
    public ResponseEntity<ApiResponse<Long>> getTotalCustomers() {
        return ResponseEntity.ok(ApiResponse.success(
                "Get total customers successfully",
                orderService.getTotalCustomersPaid()
        ));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getTotalOrders() {
        return ResponseEntity.ok(ApiResponse.success(
                "Get total orders successfully",
                orderService.getTotalOrdersPaid()
        ));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getMyOrders() {
        UserAccount currentUser = getCurrentUser();
        List<OrderDTO> orders = orderService.getOrdersDTOByUserId(currentUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
}