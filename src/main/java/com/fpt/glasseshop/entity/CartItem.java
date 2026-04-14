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
@Table(name = "cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "lens_option_id")
    private LensOption lensOption;

    private Integer quantity;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "is_lens")
    private Boolean isLens;

    @Column(name = "is_preorder")
    private Boolean isPreorder;

    private java.math.BigDecimal price;

    @Column(name = "product_name")
    private String productName;

    @OneToOne(mappedBy = "cartItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Prescription prescription;
}
