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
public class CreateProductRequest {

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

    // Nếu sản phẩm KHÔNG có nhiều biến thể
    // backend sẽ dùng số lượng này để tạo SKU mặc định.
    private Integer stockQty;

    // Nếu sản phẩm CÓ biến thể
    // danh sách này chứa màu, size, tồn kho...
    private List<CreateVariantRequest> variants;

    private List<ProductImageDto> images;

    private List<ProductSizeChartDto> sizeCharts;
}
