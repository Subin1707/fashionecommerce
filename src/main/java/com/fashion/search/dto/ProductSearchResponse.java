package com.fashion.search.dto;

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
}
