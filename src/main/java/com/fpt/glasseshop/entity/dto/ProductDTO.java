package com.fpt.glasseshop.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fpt.glasseshop.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing a product")
public class ProductDTO {
    @Schema(description = "Unique identifier of the product", example = "1")
    private Long productId;

    @Schema(description = "Type of the product (e.g., FRAME, LENS, ACCESSORY)", example = "FRAME")
    private Product.ProductType productType;

    @Schema(description = "Name of the product", example = "Ray-Ban Aviator")
    private String name;

    @Schema(description = "Brand of the product", example = "Ray-Ban")
    private String brand;

    @Schema(description = "Detailed description of the product", example = "Classic aviator sunglasses with gold frame")
    private String description;

    @Schema(description = "Whether the product supports prescription lenses", example = "true")
    @JsonProperty("isPrescriptionSupported")
    private boolean isPrescriptionSupported;

    @Schema(description = "Price of the product", example = "150.00")
    private java.math.BigDecimal price;

    @Schema(description = "Timestamp when the product was created")
    private LocalDateTime createdAt;

    @Schema(description = "List of variants for this product")
    private List<ProductVariantDTO> variants;
}
