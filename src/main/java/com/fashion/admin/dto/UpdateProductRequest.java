package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
