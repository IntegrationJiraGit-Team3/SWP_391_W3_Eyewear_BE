package com.fpt.glasseshop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariant> variants;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private String name;
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean isPrescriptionSupported;

    private BigDecimal price;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ProductType {
        FRAME, LENS, ACCESSORY
    }
}
