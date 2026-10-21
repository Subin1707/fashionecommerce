package com.fashion.search.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ProductSearchRequest {
    private String keyword;
    private String category;
    private String categoryName;
    private Long categoryId;
    private Long brandId;
    private String style;
    private String size;
    private String color;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;

    public ProductSearchRequest() {
    }
    public ProductSearchRequest(final String keyword, final String category, final String categoryName, final Long categoryId, final Long brandId, final String style, final String size, final String color, final BigDecimal minPrice, final BigDecimal maxPrice, final Double minRating) {
        this.keyword = keyword;
        this.category = category;
        this.categoryName = categoryName;
        this.categoryId = categoryId;
        this.brandId = brandId;
        this.style = style;
        this.size = size;
        this.color = color;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minRating = minRating;
    }
}
