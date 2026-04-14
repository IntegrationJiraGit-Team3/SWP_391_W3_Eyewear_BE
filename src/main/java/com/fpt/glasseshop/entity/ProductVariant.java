package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product_variant")
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long variantId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer stockQuantity;
    private String frameSize;
    private String color;
    private String material;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    private String status;

    @Builder.Default
    private Boolean active = true;
    
    @Builder.Default
    private Boolean deleted = false;
}
