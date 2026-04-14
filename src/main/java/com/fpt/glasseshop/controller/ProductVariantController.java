package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.StockResponse;
import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.ProductVariantDTO;
import com.fpt.glasseshop.entity.dto.request.VariantRequest;
import com.fpt.glasseshop.service.ProductVariantService;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("admin/products")
public class ProductVariantController {

    @Autowired
    private ProductVariantService productVariantService;
    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> createProVariant (@Valid @RequestBody VariantRequest proVariant, @PathVariable Long productId) throws BadRequestException {

        ProductVariantDTO createProVariant = productVariantService.createProductVariant(proVariant, productId);

        return ResponseEntity.ok(ApiResponse.success("Product Variant created successfully", createProVariant));
    }

    @PutMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateProductVariant(
            @PathVariable Long variantId,
            @RequestBody VariantRequest request
    ) {
        ProductVariantDTO variant =
                productVariantService.updateProductVariant(request, variantId);

        return ResponseEntity.ok(ApiResponse.success("Product Variant updated successfully", variant));
    }
//    @PutMapping("/variants/decrease/{variantId}")
//    public ResponseEntity<ApiResponse<ProductVariant>> decreaseQuantityProductVariant(
//            @PathVariable Long variantId,
//            @RequestParam Integer amount,
//            @RequestBody VariantRequest request
//    ) {
//
//        productVariantService.decreaseStockProductVariant(variantId, amount);
//        return ResponseEntity.ok(ApiResponse.success("Product Variant decreased successfully", productVariantService.getProductVariantById(variantId)));
//    }
    @PostMapping("/variants/getStock/{variantId}")
    public ResponseEntity<ApiResponse<StockResponse>> getStockProductVariant(@PathVariable Long variantId) {
        ProductVariant variant =
                productVariantService.getProductVariantById(variantId);

        StockResponse response = new StockResponse(
                variant.getVariantId(),
                variant.getStockQuantity()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Get stock successfully", response)
        );

    }
    @PutMapping("/variants/updateStock/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateQuantityProductVariant(
            @PathVariable Long variantId,
            @RequestParam Integer quantity,
            @RequestBody VariantRequest request
    ) {
        ProductVariantDTO variant =
                productVariantService.updateProductVariant(request, variantId);
        variant.setStockQuantity(quantity);
        productVariantService.updateStockProductVariant(variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Product Variant updated successfully", variant));
    }

    @PatchMapping("/variants/{variantId}/status")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateVariantStatus(
            @PathVariable Long variantId,
            @RequestParam Boolean active
    ) throws BadRequestException {

        ProductVariantDTO updatedVariant =
                productVariantService.updateVarianStatus(variantId, active);

        return ResponseEntity.ok(ApiResponse.success("Product Variant status updated successfully", updatedVariant));
    }

    @GetMapping("/variants")
    public ResponseEntity<ApiResponse<List<ProductVariantDTO>>> getAllProductVariants(ProductVariant productVariant){
        List<ProductVariantDTO> listProVariant = productVariantService.findAllProductVariants();
        return ResponseEntity.ok(ApiResponse.success("Product Variant retrieved successfully", listProVariant));
    }

    @DeleteMapping("/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariant>> deleteProductVariant(@PathVariable Long variantId){
        productVariantService.deleteProductVariant(variantId);
        return ResponseEntity.ok(ApiResponse.success("Product Variant deleted successfully", null));
    }

    @PutMapping("/variants/updateQuantity/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantDTO>> updateVariantQuantity(
            @PathVariable Long variantId,
            @RequestParam Integer quantity
    ) throws BadRequestException {
        ProductVariantDTO updatedVariant =
                productVariantService.updateQuantityProductVariant(variantId, quantity);

        return ResponseEntity.ok(
                ApiResponse.success("Product Variant quantity updated successfully", updatedVariant)
        );
    }

}
