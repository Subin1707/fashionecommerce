package com.fashion.admin.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVariantRequest {
    private String color;
    private String size;
    private Integer stockQty;
    private BigDecimal priceAdjustment;
}
