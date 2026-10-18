package com.fashion.admin.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class UpdateVariantRequest {
    private Integer stockQty;
    private BigDecimal priceAdjustment;
    private Boolean isActive;

    public UpdateVariantRequest() {
    }
    public UpdateVariantRequest(final Integer stockQty, final BigDecimal priceAdjustment, final Boolean isActive) {
        this.stockQty = stockQty;
        this.priceAdjustment = priceAdjustment;
        this.isActive = isActive;
    }
}
