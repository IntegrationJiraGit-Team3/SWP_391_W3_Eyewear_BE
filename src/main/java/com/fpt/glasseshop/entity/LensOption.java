package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "lens_option")
public class LensOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long lensOptionId;

    private String type;
    private String thickness;
    private String coating;
    private String color;
    private BigDecimal price;
}
