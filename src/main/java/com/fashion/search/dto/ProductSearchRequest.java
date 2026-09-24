package com.fashion.search.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchRequest {
    private String keyword;
    private String category;
    private String style;
    private String size;
    private String color;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
}
