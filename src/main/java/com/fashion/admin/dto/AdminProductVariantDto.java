package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AdminProductVariantDto {
    private Long id;
    private String sku;
    private String color;
    private String size;
    private Integer stockQty;
    private BigDecimal priceAdjustment;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AdminProductVariantDto() {
    }
    public AdminProductVariantDto(final Long id, final String sku, final String color, final String size, final Integer stockQty, final BigDecimal priceAdjustment, final Boolean isActive, final LocalDateTime createdAt, final LocalDateTime updatedAt) {
        this.id = id;
        this.sku = sku;
        this.color = color;
        this.size = size;
        this.stockQty = stockQty;
        this.priceAdjustment = priceAdjustment;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
