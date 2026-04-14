package com.fpt.glasseshop.repository;

import com.fpt.glasseshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProduct_ProductId(Long productId);

    @Query("""
    SELECT COUNT(v) > 0
    FROM ProductVariant v
    WHERE v.product.productId = :productId
    AND LOWER(v.frameSize) = LOWER(:frameSize)
    AND LOWER(v.color) = LOWER(:color)
    AND LOWER(v.material) = LOWER(:material)
""")
    boolean existsVariant(Long productId, String frameSize, String color, String material);

    List<ProductVariant> findByProduct_ProductIdAndActiveTrue(Long productId);
    List<ProductVariant> findByActiveTrue();

    List<ProductVariant> findByDeletedFalse();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :quantity WHERE v.variantId = :variantId AND v.stockQuantity >= :quantity")
    int decreaseStock(@org.springframework.data.repository.query.Param("variantId") Long variantId, @org.springframework.data.repository.query.Param("quantity") int quantity);


    List<ProductVariant> findByProduct_ProductIdAndActiveTrueAndDeletedFalse(Long productId);

;
}
