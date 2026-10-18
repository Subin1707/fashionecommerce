package com.fashion.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ProductImageDto {

    private Long id;

    private Long variantId;

    private String imageUrl;

    private String altText;

    @JsonProperty("isPrimary")
    private Boolean isPrimary;

    private Integer displayOrder;

    private LocalDateTime createdAt;

    public ProductImageDto() {
    }
    public ProductImageDto(final Long id, final Long variantId, final String imageUrl, final String altText, final Boolean isPrimary, final Integer displayOrder, final LocalDateTime createdAt) {
        this.id = id;
        this.variantId = variantId;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.isPrimary = isPrimary;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }
}
