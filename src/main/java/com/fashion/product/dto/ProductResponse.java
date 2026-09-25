package com.fashion.product.dto;

import com.fashion.product.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String shortDescription;
    private String description;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private String material;
    private String fit;
    private String gender;
    private String status;
    private Boolean isFeatured;
    private Boolean isNew;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private String primaryImageUrl;
    private List<String> imageUrls;
    private List<String> sizes;
    private List<String> colors;
    private Integer totalStock;
    @lombok.Setter
    private Double averageRating;
    @lombok.Setter
    private Long totalReviews;

    public static ProductResponse fromEntity(Product product) {
        return fromEntity(product, null);
    }

    public static ProductResponse fromEntity(Product product, String primaryImageUrl) {
        return fromEntity(product, primaryImageUrl, primaryImageUrl == null ? List.of() : List.of(primaryImageUrl));
    }

    public static ProductResponse fromEntity(Product product, String primaryImageUrl, List<String> imageUrls) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .salePrice(product.getSalePrice())
                .material(product.getMaterial())
                .fit(product.getFit())
                .gender(product.getGender())
                .status(product.getStatus())
                .isFeatured(product.getIsFeatured())
                .isNew(product.getIsNew())
                .brandId(product.getBrand() == null ? null : product.getBrand().getId())
                .brandName(product.getBrandName())
                .categoryId(product.getCategory() == null ? null : product.getCategory().getId())
                .categoryName(product.getCategory() == null ? null : product.getCategory().getName())
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(imageUrls == null ? List.of() : imageUrls)
                .sizes(product.getVariants() == null ? List.of() : product.getVariants().stream()
                        .map(variant -> variant.getSize()).filter(java.util.Objects::nonNull).distinct().toList())
                .colors(product.getVariants() == null ? List.of() : product.getVariants().stream()
                        .map(variant -> variant.getColor()).filter(java.util.Objects::nonNull).distinct().toList())
                .totalStock(product.getAvailableQuantity())
                .build();
    }
}
