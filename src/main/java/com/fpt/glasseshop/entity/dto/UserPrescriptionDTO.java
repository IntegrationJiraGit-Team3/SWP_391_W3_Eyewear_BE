package com.fpt.glasseshop.entity.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UserPrescriptionDTO {

    private String name;

    private BigDecimal sphLeft;
    private BigDecimal sphRight;
    private BigDecimal cylLeft;
    private BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private BigDecimal addLeft;
    private BigDecimal addRight;

}