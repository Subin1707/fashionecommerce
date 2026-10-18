package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class UpdateProductRequest {
    private Long brandId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private Boolean isFeatured;
    private Boolean isNew;
    private List<ProductImageDto> images;
    private List<ProductSizeChartDto> sizeCharts;

    public UpdateProductRequest() {
    }
    public UpdateProductRequest(final Long brandId, final Long categoryId, final String name, final String description, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final Boolean isFeatured, final Boolean isNew, final List<ProductImageDto> images, final List<ProductSizeChartDto> sizeCharts) {
        this.brandId = brandId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.material = material;
        this.fit = fit;
        this.gender = gender;
        this.status = status;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
        this.images = images;
        this.sizeCharts = sizeCharts;
    }
}
