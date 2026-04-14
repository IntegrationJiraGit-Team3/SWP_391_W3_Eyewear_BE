package com.fpt.glasseshop.service;

import com.fpt.glasseshop.entity.Product;
import com.fpt.glasseshop.entity.ProductVariant;
import com.fpt.glasseshop.entity.dto.ProductDTO;
import com.fpt.glasseshop.entity.dto.ProductVariantDTO;
import com.fpt.glasseshop.repository.ProductRepository;
import com.fpt.glasseshop.repository.ProductVariantRepository;
import com.fpt.glasseshop.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;

    // ✅ GET ALL
    //admin
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    //update
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // update only fields you want
        product.setName(productDTO.getName());
        product.setBrand(productDTO.getBrand());
        product.setDescription(productDTO.getDescription());
        product.setProductType(productDTO.getProductType());
        product.setPrescriptionSupported(productDTO.isPrescriptionSupported());
        product.setPrice(productDTO.getPrice());

        return convertToDTO(productRepository.save(product));
    }
    public List<ProductDTO> getAllProductsForUser() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToDTOForUser)
                .collect(Collectors.toList());
    }

    // ✅ SEARCH (Trả DTO luôn cho đồng bộ)
    public List<ProductDTO> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return productRepository
                .findAllByNameContainingIgnoreCaseOrBrandContainingIgnoreCase(query, query)
                .stream()
                .distinct() // bỏ duplicate
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ✅ GET PRODUCT DETAIL (Entity để dùng nội bộ)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    // ✅ GET VARIANTS
    public List<ProductVariant> getVariantsByProductId(Long id) {
        return productVariantRepository.findByProduct_ProductId(id);
    }

    public ProductDTO getProductDTOById(Long id) {
        return productRepository.findById(id)
                .map(this::convertToDTO)
                .orElse(null);
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = Product.builder()
                .name(productDTO.getName())
                .brand(productDTO.getBrand())
                .description(productDTO.getDescription())
                .productType(productDTO.getProductType())
                .isPrescriptionSupported(productDTO.isPrescriptionSupported())
                .price(productDTO.getPrice())
                .build();
        Product savedProduct = productRepository.save(product);

        if (productDTO.getVariants() != null && !productDTO.getVariants().isEmpty()) {
            List<ProductVariant> variants = productDTO.getVariants().stream()
                    .map(vDto -> ProductVariant.builder()
                            .product(savedProduct)
                            .stockQuantity(vDto.getStockQuantity())
                            .frameSize(vDto.getFrameSize())
                            .color(vDto.getColor())
                            .material(vDto.getMaterial())
                            .imageUrl(vDto.getImageUrl())
                            .status(vDto.getStatus() != null ? vDto.getStatus() : "AVAILABLE")
                            .build())
                    .collect(Collectors.toList());
            productVariantRepository.saveAll(variants);
            savedProduct.setVariants(variants);
        }

        return convertToDTO(savedProduct);
    }

    // ✅ DELETE PRODUCT
    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }

        long orderItemCount = orderItemRepository.countByProductId(id);

        if (orderItemCount > 0) {
            throw new RuntimeException(
                    "Cannot delete product with id: " + id +
                            ". Product has " + orderItemCount + " order item(s).");
        }

        productRepository.deleteById(id);
    }

    // ================== MAPPING ==================
    //for admin
    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .productId(product.getProductId())
                .productType(product.getProductType())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .isPrescriptionSupported(product.isPrescriptionSupported())
                .price(product.getPrice())
                .createdAt(product.getCreatedAt())
                .variants(product.getVariants() != null
                        ? product.getVariants().stream()
                        .filter(d -> Boolean.FALSE.equals(d.getDeleted()))//show only deleted = fasle
                                  .map(this::mapToVariantDTO)
                                .collect(Collectors.toList())
                        : java.util.Collections.emptyList())
                .build();
    }

    private ProductVariantDTO mapToVariantDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .variantId(variant.getVariantId())
                .productId(
                        variant.getProduct() != null
                                ? variant.getProduct().getProductId()
                                : null)
                .stockQuantity(variant.getStockQuantity())
                .frameSize(variant.getFrameSize())
                .color(variant.getColor())
                .material(variant.getMaterial())
                .imageUrl(variant.getImageUrl())
                .status(variant.getStatus())
                .active(variant.getActive())
                .deleted(variant.getDeleted())
                .build();
    }
    //for user
    private ProductDTO convertToDTOForUser(Product product) {

        List<ProductVariantDTO> variants = productVariantRepository
                .findByProduct_ProductIdAndActiveTrueAndDeletedFalse(product.getProductId())//show only active variant and deleted =false
                .stream()
                .map(this::mapToVariantDTO)
                .collect(Collectors.toList());

        return ProductDTO.builder()
                .productId(product.getProductId())
                .productType(product.getProductType())
                .name(product.getName())
                .brand(product.getBrand())
                .description(product.getDescription())
                .isPrescriptionSupported(product.isPrescriptionSupported())
                .price(product.getPrice())
                .createdAt(product.getCreatedAt())
                .variants(variants)
                .build();
    }

}