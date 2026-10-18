package com.fashion.admin.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
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

    public CreateProductRequest() {
    }
    public CreateProductRequest(final Long brandId, final Long categoryId, final String name, final String description, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final Boolean isFeatured, final Boolean isNew, final Integer stockQty, final List<CreateVariantRequest> variants, final List<ProductImageDto> images, final List<ProductSizeChartDto> sizeCharts) {
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
        this.stockQty = stockQty;
        this.variants = variants;
        this.images = images;
        this.sizeCharts = sizeCharts;
    }
}
