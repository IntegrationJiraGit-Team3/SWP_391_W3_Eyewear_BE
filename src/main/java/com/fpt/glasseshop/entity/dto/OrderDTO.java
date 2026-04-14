package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing an order")
public class OrderDTO {
    @Schema(description = "Unique identifier of the order", example = "101")
    private Long orderId;

    @Schema(description = "Human readable order code", example = "ORD-12345")
    private String orderCode;

    @Schema(description = "ID of the user who placed the order", example = "5")
    private Long userId;

    @Schema(description = "Name of the user who placed the order", example = "John Doe")
    private String userName;

    @Schema(description = "Email of the user who placed the order", example = "john.doe@example.com")
    private String userEmail;

    @Schema(description = "Timestamp when the order was placed")
    private LocalDateTime orderDate;

    @Schema(description = "Current status of the order", example = "PENDING")
    private String status;

    @Schema(description = "Total price of the order", example = "250.00")
    private BigDecimal totalPrice;

    private String fullName;
    private String phone;
    private String address;
    private String note;
    
    private BigDecimal shippingFee;
    private BigDecimal voucherDiscount;
    private BigDecimal finalPrice;
    private BigDecimal depositAmount;
    private String depositType;
    private String depositPaymentMethod;
    private LocalDateTime stockReadyAt;

    @Schema(description = "Payment status of the order", example = "PAID")
    private String paymentStatus;

    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "List of items in the order")
    private List<OrderItemDTO> orderItems;

    @Schema(description = "Total number of items in the order", example = "3")
    private Integer totalItems;
}
