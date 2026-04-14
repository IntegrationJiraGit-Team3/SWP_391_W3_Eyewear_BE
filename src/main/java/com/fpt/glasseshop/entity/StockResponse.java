package com.fpt.glasseshop.entity;


public class StockResponse {
    private Long variantId;
    private Integer stockQuantity;

    public StockResponse(Long variantId, Integer stockQuantity) {
        this.variantId = variantId;
        this.stockQuantity = stockQuantity;
    }

    // getters

    public Long getVariantId() {
        return variantId;
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}