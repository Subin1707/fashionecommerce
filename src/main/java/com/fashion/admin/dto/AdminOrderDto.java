package com.fashion.admin.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class AdminOrderDto {

    private Long id;

    private Long userId;

    private Double totalAmount;
    private Double vatAmount;

    private Double shippingFee;

    private Double discountAmount;

    private Double finalAmount;

    private String shippingAddress;

    private String paymentMethod;

    private String paymentStatus;

    private String orderStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    public AdminOrderDto() {
    }
    public AdminOrderDto(final Long id, final Long userId, final Double totalAmount, final Double vatAmount, final Double shippingFee, final Double discountAmount, final Double finalAmount, final String shippingAddress, final String paymentMethod, final String paymentStatus, final String orderStatus, final LocalDateTime createdAt, final LocalDateTime updatedAt, final LocalDateTime completedAt) {
        this.id = id;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.vatAmount = vatAmount;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
    }
}
