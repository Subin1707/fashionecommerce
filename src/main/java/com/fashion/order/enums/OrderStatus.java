package com.fashion.order.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    RETURNED,
    CANCELLED;

    public static OrderStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ");
        }

        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }

        throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + value);
    }
}
