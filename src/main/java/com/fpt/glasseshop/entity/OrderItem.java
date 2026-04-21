package com.fpt.glasseshop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "order_item")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderItemId;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "lens_option_id")
    private LensOption lensOption;

    private Integer quantity;
    private BigDecimal unitPrice;
    private String fulfillmentType;

    @Column(name = "snapshot_variant_id")
    private Long variantId;

    @Column(name = "snapshot_product_id")
    private Long productId;

    @Column(name = "snapshot_product_name")
    private String productName;

    @Column(name = "snapshot_variant_color")
    private String variantColor;

    @Column(name = "snapshot_variant_size")
    private String variantSize;

    @Column(name = "snapshot_image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "snapshot_lens_type")
    private String lensType;

    @Column(name = "snapshot_lens_price")
    private BigDecimal lensPrice;

    @Column(name = "snapshot_lens_coating")
    private String lensCoating;

    @Column(name = "snapshot_lens_option_id")
    private Long lensOptionId;

    // Prescription Data
    private BigDecimal sphLeft;
    private BigDecimal sphRight;
    private BigDecimal cylLeft;
    private BigDecimal cylRight;
    private Integer axisLeft;
    private Integer axisRight;
    private BigDecimal addLeft;
    private BigDecimal addRight;

    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL)
    private Prescription prescription;

    @OneToOne(mappedBy = "orderItem", cascade = CascadeType.ALL)
    private PreOrder preOrder;

    @Column(name = "is_preorder")
    private Boolean isPreorder;

    @Column(name = "stock_deducted")
    private Boolean stockDeducted = false;

    public String getItemType() {
        if (prescription != null || sphLeft != null || sphRight != null ||
                cylLeft != null || cylRight != null || axisLeft != null || axisRight != null)
            return "PRESCRIPTION";
        if (Boolean.TRUE.equals(isPreorder) || preOrder != null)
            return "PRE_ORDER";
        return "IN_STOCK";
    }

    private String groupId; // ràng buộc combo
}