package com.fashion.order.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CheckoutSummary {
    private Double subtotal;
    private Double shippingFee;
    private Double discountAmount;
    private Double finalAmount;

    public CheckoutSummary() {
    }
    public CheckoutSummary(final Double subtotal, final Double shippingFee, final Double discountAmount, final Double finalAmount) {
        this.subtotal = subtotal;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }
}
