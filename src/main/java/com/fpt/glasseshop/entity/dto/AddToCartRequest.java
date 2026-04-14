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
@Schema(description = "Request object to add an item to the cart")
public class AddToCartRequest {
    @NotNull(message = "Variant ID is required")
    @Schema(description = "ID of the product variant", example = "1")
    private Long variantId;
    @Schema(description = "ID of the product", example = "10")
    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity of the item", example = "1")
    private Integer quantity;

    @Schema(description = "ID of the lens option (optional)", example = "1")
    private Long lensOptionId;

    @Schema(description = "Whether it is a lens", example = "false")
    private Boolean isLens;

    @Schema(description = "Whether it is a preorder", example = "false")
    private Boolean isPreorder;

    @Schema(description = "ID of the saved prescription (optional)", example = "1")
    private Long prescriptionId;

    // Prescription fields
    private java.math.BigDecimal sphLeft;
    private java.math.BigDecimal sphRight;
    private java.math.BigDecimal cylLeft;
    private java.math.BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private java.math.BigDecimal addLeft;
    private java.math.BigDecimal addRight;

}
