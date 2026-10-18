package com.fashion.order.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    private String shippingAddress;
    private Double shippingLat;
    private Double shippingLng;
    private String paymentMethod;
    private Double shippingFee;
    private Double discountAmount;
    private String voucherCode;
    private String note;

    public CreateOrderRequest() {
    }
    public CreateOrderRequest(final String shippingAddress, final Double shippingLat, final Double shippingLng, final String paymentMethod, final Double shippingFee, final Double discountAmount, final String voucherCode, final String note) {
        this.shippingAddress = shippingAddress;
        this.shippingLat = shippingLat;
        this.shippingLng = shippingLng;
        this.paymentMethod = paymentMethod;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.voucherCode = voucherCode;
        this.note = note;
    }
}
