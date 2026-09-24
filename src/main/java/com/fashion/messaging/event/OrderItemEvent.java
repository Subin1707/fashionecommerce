package com.fashion.messaging.event;

public record OrderItemEvent(
        Long productId,
        Long variantId,
        String sku,
        Integer quantity,
        Double price) {
}
