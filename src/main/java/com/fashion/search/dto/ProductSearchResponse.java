package com.fashion.search.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ProductSearchResponse {
    private Long id;
    private String name;
    private String description;
    private String slug;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private String brandName;
    private String categoryName;
    private String primaryImageUrl;
    private List<String> colors;
    private List<String> sizes;
    private Integer totalStock;
    private Double averageRating;
    private Integer totalReviews;
    private Boolean isFeatured;
    private Boolean isNew;

    public ProductSearchResponse() {
    }
    public ProductSearchResponse(final Long id, final String name, final String description, final String slug, final BigDecimal basePrice, final BigDecimal salePrice, final String material, final String fit, final String gender, final String status, final String brandName, final String categoryName, final String primaryImageUrl, final List<String> colors, final List<String> sizes, final Integer totalStock, final Double averageRating, final Integer totalReviews, final Boolean isFeatured, final Boolean isNew) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.slug = slug;
        this.basePrice = basePrice;
        this.salePrice = salePrice;
        this.material = material;
        this.fit = fit;
        this.gender = gender;
        this.status = status;
        this.brandName = brandName;
        this.categoryName = categoryName;
        this.primaryImageUrl = primaryImageUrl;
        this.colors = colors;
        this.sizes = sizes;
        this.totalStock = totalStock;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.isFeatured = isFeatured;
        this.isNew = isNew;
    }
}
