package com.fpt.glasseshop.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing an item in an order")
public class OrderItemDTO {
    @Schema(description = "Unique identifier of the order item", example = "501")
    private Long orderItemId;

    @Schema(description = "ID of the product variant", example = "10")
    private Long variantId;

    @Schema(description = "ID of the product", example = "5")
    private Long productId;

    @Schema(description = "Name of the product", example = "Ray-Ban Aviator")
    private String productName;

    @Schema(description = "Color of the product variant", example = "Gold")
    private String variantColor;

    @Schema(description = "Size of the frame", example = "Medium")
    private String variantSize;

    @Schema(description = "URL of the product image")
    private String imageUrl;

    @Schema(description = "Type of lens selected", example = "Single Vision")
    private String lensType;

    @Schema(description = "Price of the lens at purchase", example = "30.00")
    private BigDecimal lensPrice;

    @Schema(description = "Coating applied to the lens", example = "Anti-Reflective")
    private String lensCoating;

    @Schema(description = "ID of the lens option", example = "3")
    private Long lensOptionId;

    @Schema(description = "Quantity ordered", example = "1")
    private Integer quantity;

    @Schema(description = "Price per unit", example = "150.00")
    private BigDecimal unitPrice;

    @Schema(description = "Subtotal for this item (quantity * unitPrice)", example = "150.00")
    private BigDecimal subtotal;

    @Schema(description = "Fulfillment type (e.g., SHIP_TO_HOME, PICKUP)", example = "SHIP_TO_HOME")
    private String fulfillmentType;

    @Schema(description = "Type of item (PRESCRIPTION, PRE_ORDER, IN_STOCK)", example = "IN_STOCK")
    private String itemType;

    @Schema(description = "Whether it is a preorder", example = "false")
    private Boolean isPreorder;

    // Prescription Data (Flat parameters from OrderItem entity)
    @Schema(description = "Spherical (Left)", example = "-1.50")
    private BigDecimal sphLeft;
    @Schema(description = "Spherical (Right)", example = "-1.75")
    private BigDecimal sphRight;
    @Schema(description = "Cylindrical (Left)", example = "-0.25")
    private BigDecimal cylLeft;
    @Schema(description = "Cylindrical (Right)", example = "-0.50")
    private BigDecimal cylRight;
    @Schema(description = "Axis (Left)", example = "10")
    private Integer axisLeft;
    @Schema(description = "Axis (Right)", example = "15")
    private Integer axisRight;
    @Schema(description = "Addition (Left)", example = "1.00")
    private BigDecimal addLeft;
    @Schema(description = "Addition (Right)", example = "1.00")
    private BigDecimal addRight;


    @Schema(description = "Prescription details if applicable")
    private PrescriptionDTO prescription;
}
