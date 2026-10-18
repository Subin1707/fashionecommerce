package com.fashion.order.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    private String shippingAddress;
    private Double shippingLat;
    private Double shippingLng;
    private String paymentMethod;
    private Double shippingFee;
    private Double discountAmount;
    private String voucherCode;
    private Long productId;
    private Long variantId;
    private String size;
    private Integer quantity;

    public CheckoutRequest() {
    }
    public CheckoutRequest(final String shippingAddress, final Double shippingLat, final Double shippingLng, final String paymentMethod, final Double shippingFee, final Double discountAmount, final String voucherCode, final Long productId, final Long variantId, final String size, final Integer quantity) {
        this.shippingAddress = shippingAddress;
        this.shippingLat = shippingLat;
        this.shippingLng = shippingLng;
        this.paymentMethod = paymentMethod;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.voucherCode = voucherCode;
        this.productId = productId;
        this.variantId = variantId;
        this.size = size;
        this.quantity = quantity;
    }
}
