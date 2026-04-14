package com.fpt.glasseshop.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request object to process a payment")
public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    @Schema(description = "ID of the order to pay for", example = "1")
    private Long orderId;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Method of payment", example = "CREDIT_CARD")
    private String paymentMethod;

    @Schema(description = "Mock card number or token", example = "1234-5678-9012-3456")
    private String paymentToken;
}
