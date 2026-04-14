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
@Table(name = "product_image")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ImageType imageType;

    private Integer displayOrder;

    public enum ImageType {
        FRONT, SIDE, ANGLE, MODEL_3D
    }
}
