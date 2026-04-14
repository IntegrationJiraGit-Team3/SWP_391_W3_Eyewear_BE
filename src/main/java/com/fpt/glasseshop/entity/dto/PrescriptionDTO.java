package com.fpt.glasseshop.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDTO {
    private Long prescriptionId;
    private Long orderItemId;
    
    private String name;

    @NotNull(message = "Left eye SPH is required")
    private BigDecimal sphLeft;

    @NotNull(message = "Right eye SPH is required")
    private BigDecimal sphRight;

    private BigDecimal cylLeft;
    private BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private BigDecimal addLeft;
    private BigDecimal addRight;



    private String doctorName;
    private LocalDate expirationDate;
    private Boolean status;
    private String adminNote;
    private LocalDateTime createdAt;
}
