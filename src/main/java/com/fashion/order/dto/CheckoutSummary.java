package com.fashion.order.dto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CheckoutSummary {
    private Double subtotal;
    private Double vatAmount;
    private Double shippingFee;
    private Double discountAmount;
    private Double finalAmount;

    public CheckoutSummary() {
    }
    public CheckoutSummary(final Double subtotal, final Double shippingFee, final Double discountAmount, final Double finalAmount) {
        this(subtotal, 0.0, shippingFee, discountAmount, finalAmount);
    }
    public CheckoutSummary(final Double subtotal, final Double vatAmount, final Double shippingFee, final Double discountAmount, final Double finalAmount) {
        this.subtotal = subtotal;
        this.vatAmount = vatAmount;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
    }
}
