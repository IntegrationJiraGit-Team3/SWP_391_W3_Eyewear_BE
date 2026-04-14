package com.fpt.glasseshop.controller;

import com.fpt.glasseshop.entity.dto.ApiResponse;
import com.fpt.glasseshop.entity.dto.ProductDTO;
import com.fpt.glasseshop.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "ProductAPI", description = "Operations related to products")
@Slf4j
public class ProductRestController {

        private final ProductService productService;

        // ================= GET ALL PRODUCTS =================
        @GetMapping("/admin")
        @Operation(summary = "Get all products", description = "Retrieves a list of all available products")
        public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts() {

                List<ProductDTO> products = productService.getAllProducts();

                return ResponseEntity.ok(
                                ApiResponse.success("Products retrieved successfully", products));
        }

        @GetMapping
        @Operation(summary = "Get all products", description = "Retrieves a list of all available products")
        public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProductsForUser() {

            List<ProductDTO> products = productService.getAllProductsForUser();

            return ResponseEntity.ok(
                    ApiResponse.success("Products retrieved successfully", products));
        }
        //================== UPDATE PRODUCT =================
        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
                @Parameter(description = "Product ID", example = "1")
                @PathVariable Long id,

                @RequestBody ProductDTO productDTO) {

                productDTO.setProductId(id);

                ProductDTO updatedProduct = productService.updateProduct(id,productDTO);

                return ResponseEntity.ok(
                        ApiResponse.success("Product updated successfully", updatedProduct)
                );
        }
        // ================= GET PRODUCT BY ID =================
        @GetMapping("/{id}")
        @Operation(summary = "Get product by ID", description = "Retrieves a single product by its unique identifier")
        public ResponseEntity<ApiResponse<ProductDTO>> getProductById(
                        @Parameter(description = "ID of the product", example = "1") @PathVariable Long id) {

                ProductDTO product = productService.getProductDTOById(id);

                if (product != null) {
                        return ResponseEntity.ok(
                                        ApiResponse.success("Product retrieved successfully", product));
                }

                return ResponseEntity.status(404)
                                .body(ApiResponse.error("Product not found"));
        }

        // ================= SEARCH PRODUCTS =================
        @GetMapping("/search")
        @Operation(summary = "Search products", description = "Search products by name or brand")
        public ResponseEntity<ApiResponse<List<ProductDTO>>> searchProducts(
                        @Parameter(description = "Search query", example = "RayBan") @RequestParam String query) {

                List<ProductDTO> results = productService.searchProducts(query);

                return ResponseEntity.ok(
                                ApiResponse.success("Search completed successfully", results));
        }

        // ================= CREATE PRODUCT =================
        @PostMapping
        @Operation(summary = "Create a new product", description = "Adds a new product to the catalog")
        public ResponseEntity<ApiResponse<ProductDTO>> createProduct(
                        @RequestBody ProductDTO productDTO) {

                log.info("Received request to create product: {}", productDTO);

                ProductDTO created = productService.createProduct(productDTO);

                log.info("Successfully created product with ID: {}", created.getProductId());

                return ResponseEntity.status(201)
                                .body(ApiResponse.success("Product created successfully", created));
        }

        // ================= DELETE PRODUCT =================
        @DeleteMapping("/{id}")
        @Operation(summary = "Delete product", description = "Deletes a product from catalog by ID")
        public ResponseEntity<ApiResponse<Void>> deleteProduct(
                        @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {

                try {

                        productService.deleteProduct(id);

                        return ResponseEntity.ok(
                                        ApiResponse.success("Product deleted successfully", null));

                } catch (RuntimeException e) {

                        log.error("Error deleting product: {}", e.getMessage());

                        return ResponseEntity.status(404)
                                        .body(ApiResponse.error(e.getMessage()));
                }
        }
}