package com.fashion.admin.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderDto {

    private Long id;

    private Long userId;

    private Double totalAmount;

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
}
