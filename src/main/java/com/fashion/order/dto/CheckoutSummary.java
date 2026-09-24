package com.fashion.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutSummary {
    private Double subtotal;
    private Double shippingFee;
    private Double discountAmount;
    private Double finalAmount;
}
