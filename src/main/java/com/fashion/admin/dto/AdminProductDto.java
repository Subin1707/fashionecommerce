package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
