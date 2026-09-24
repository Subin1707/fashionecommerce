package com.fashion.product.dto;

import com.fashion.product.entity.ProductVariant;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductVariantResponse {
    private Long id;
    private String sku;
    private String color;
    private String size;
    private Integer stockQty;
    private BigDecimal priceAdjustment;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductVariantResponse fromEntity(ProductVariant variant) {
        return ProductVariantResponse.builder()
                .id(variant.getId())
                .sku(variant.getSku())
                .color(variant.getColor())
                .size(variant.getSize())
                .stockQty(variant.getStockQty())
                .priceAdjustment(variant.getPriceAdjustment())
                .isActive(variant.getIsActive())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }
}