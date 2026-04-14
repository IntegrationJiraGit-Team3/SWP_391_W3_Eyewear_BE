package com.fpt.glasseshop.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request object for an item within an order")
public class OrderItemRequest {
    @NotNull(message = "Variant ID is required")
    @Schema(description = "ID of the product variant", example = "1")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the item", example = "1")
    private Integer quantity;

    @Schema(description = "ID of the lens option (optional)", example = "1")
    private Long lensOptionId;
    private PrescriptionDTO prescription;
}
