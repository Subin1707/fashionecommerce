package com.fashion.shipping.enums;

public enum ShipmentStatus {
    CREATED,
    WAITING_FOR_PICKUP,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_FAILED,
    RETURNING,
    RETURNED;

    public static ShipmentStatus fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Trạng thái vận chuyển không hợp lệ");
        }
        for (ShipmentStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status;
            }
        }
        throw new IllegalArgumentException("Trạng thái vận chuyển không hợp lệ: " + value);
    }
}