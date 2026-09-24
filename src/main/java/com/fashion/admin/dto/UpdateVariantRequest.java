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
public class UpdateVariantRequest {
    private Integer stockQty;
    private BigDecimal priceAdjustment;
    private Boolean isActive;
}
