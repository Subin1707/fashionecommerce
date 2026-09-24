package com.fashion.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String shippingAddress;
    private Double shippingLat;
    private Double shippingLng;
    private String paymentMethod;
    private Double shippingFee;
    private Double discountAmount;
    private String voucherCode;
    private String note;
}
