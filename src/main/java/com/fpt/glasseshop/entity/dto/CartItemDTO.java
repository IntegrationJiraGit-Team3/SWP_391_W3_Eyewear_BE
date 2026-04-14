package com.fpt.glasseshop.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing an item in a shopping cart")
public class CartItemDTO {
    @Schema(description = "ID of the cart item", example = "10")
    private Long cartItemId;

    @Schema(description = "ID of the product variant", example = "1")
    private Long variantId;

    @Schema(description = "ID of the product", example = "1")
    private Long productId;

    @Schema(description = "Whether it is a lens", example = "false")
    private Boolean isLens;

    @Schema(description = "Whether it is a preorder", example = "false")
    private Boolean isPreorder;

    @Schema(description = "Name of the product", example = "Classic Aviator")
    private String productName;

    @Schema(description = "Color of the variant", example = "Gold")
    private String variantColor;

    @Schema(description = "Size of the frame", example = "Medium")
    private String variantSize;

    @Schema(description = "URL of the variant image", example = "https://example.com/image.jpg")
    private String imageUrl;

    @Schema(description = "Quantity of the item", example = "1")
    private Integer quantity;

    @Schema(description = "Price for a single unit (including lens option)", example = "180.00")
    private BigDecimal unitPrice;

    @Schema(description = "Total price for this item line (unitPrice * quantity)", example = "180.00")
    private BigDecimal subtotal;

    // Prescription Data for Manual Entry
    private BigDecimal sphLeft;
    private BigDecimal sphRight;
    private BigDecimal cylLeft;
    private BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private BigDecimal addLeft;
    private BigDecimal addRight;


    @Schema(description = "Prescription details if applicable")
    private PrescriptionDTO prescription;
}
