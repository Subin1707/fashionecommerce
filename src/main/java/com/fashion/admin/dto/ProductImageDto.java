package com.fashion.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
