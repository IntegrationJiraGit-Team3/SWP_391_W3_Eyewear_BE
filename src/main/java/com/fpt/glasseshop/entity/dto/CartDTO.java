package com.fpt.glasseshop.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing a shopping cart")
public class CartDTO {
    @Schema(description = "ID of the cart", example = "1")
    private Long cartId;

    @Schema(description = "ID of the user", example = "5")
    private Long userId;

    @Schema(description = "List of items in the cart")
    private List<CartItemDTO> items;

    @Schema(description = "Total price of all items in the cart", example = "150.00")
    private BigDecimal totalPrice;

    @Schema(description = "Total number of items in the cart", example = "2")
    private Integer totalItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
