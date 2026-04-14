package com.fpt.glasseshop.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "ID of the order being reviewed", example = "101")
    private Long orderId;

    @NotNull(message = "Product ID is required")
    @Schema(description = "ID of the specific product to review (must be in that order)", example = "10")
    private Long productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Rating score (1-5)", example = "5")
    private Integer rating;

    @NotBlank(message = "Review text cannot be blank")
    @Schema(description = "The review text content", example = "I love these frames!")
    private String comment;
}
