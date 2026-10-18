package com.fashion.admin.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class CreateVariantRequest {
    private String color;
    private String size;
    private Integer stockQty;
    private BigDecimal priceAdjustment;

    public CreateVariantRequest() {
    }
    public CreateVariantRequest(final String color, final String size, final Integer stockQty, final BigDecimal priceAdjustment) {
        this.color = color;
        this.size = size;
        this.stockQty = stockQty;
        this.priceAdjustment = priceAdjustment;
    }
}
