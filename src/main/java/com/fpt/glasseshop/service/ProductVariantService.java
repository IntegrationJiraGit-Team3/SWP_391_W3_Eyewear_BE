package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.dto.ProductVariantDTO;
import com.fpt.glasseshop.entity.dto.request.VariantRequest;
import org.apache.coyote.BadRequestException;

import java.util.List;

public interface ProductVariantService {
    //Get variantId
    ProductVariant getProductVariantById(Long productVariantId);

    //Create variant
    ProductVariantDTO createProductVariant(VariantRequest request, Long productId) throws BadRequestException;

    //List variant
    List<ProductVariantDTO> findAllProductVariants();

    //Update toàn bộ variant
    ProductVariantDTO updateProductVariant(VariantRequest productVariant, Long productVariantId );

    //Update riêng stock của variant (hết hàng, nhập kho, sai số)
    ProductVariantDTO updateStockProductVariant(Long productVariantId, Integer stockQty );

    //Hết kho ẩn variant khỏi người dùng
    ProductVariantDTO updateVarianStatus(Long productVariantId, Boolean active) throws BadRequestException;


    ProductVariantDTO decreaseStockProductVariant(Long productVariantId, Integer amount);

    //Delete variant
    void deleteProductVariant(Long productVariantId);

    ProductVariantDTO updateQuantityProductVariant(Long variantId, Integer quantity) throws BadRequestException;
}
