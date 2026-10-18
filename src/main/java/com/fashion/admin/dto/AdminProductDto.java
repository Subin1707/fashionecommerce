package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AdminProductDto {
    private Long id;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String slug;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private Boolean isFeatured;
    private Boolean isNew;
    private Long totalStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AdminProductVariantDto> variants;
    private List<ProductImageDto> images;
    private List<ProductSizeChartDto> sizeCharts;

    public AdminProductDto() {
    }
    public AdminProductDto(final Long id, final Long brandId, final String brandName, final Long categoryId, final String categoryName, final String name, final String slug, final String description, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final Boolean isFeatured, final Boolean isNew, final Long totalStock, final LocalDateTime createdAt, final LocalDateTime updatedAt, final List<AdminProductVariantDto> variants, final List<ProductImageDto> images, final List<ProductSizeChartDto> sizeCharts) {
        this.id = id;
        this.brandId = brandId;
        this.brandName = brandName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.material = material;
        this.fit = fit;
        this.gender = gender;
        this.status = status;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
        this.totalStock = totalStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.variants = variants;
        this.images = images;
        this.sizeCharts = sizeCharts;
    }
}
