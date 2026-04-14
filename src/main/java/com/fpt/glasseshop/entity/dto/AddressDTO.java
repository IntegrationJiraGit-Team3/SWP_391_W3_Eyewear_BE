package com.fpt.glasseshop.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Data Transfer Object representing an address")
public class AddressDTO {
    @Schema(description = "Unique identifier of the address", example = "201")
    private Long addressId;

    @Schema(description = "ID of the user this address belongs to", example = "5")
    private Long userId;

    @NotBlank(message = "Street is required")
    @Schema(description = "Street name and number", example = "123 Main St")
    private String street;

    @NotBlank(message = "City is required")
    @Schema(description = "City name", example = "Metropolis")
    private String city;

    @NotBlank(message = "State is required")
    @Schema(description = "State/Province name", example = "NY")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Schema(description = "Zip/Postal code", example = "10001")
    private String zipCode;

    @NotBlank(message = "Country is required")
    @Schema(description = "Country name", example = "USA")
    private String country;
}
