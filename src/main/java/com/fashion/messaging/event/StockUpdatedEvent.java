package com.fashion.messaging.event;

import java.time.Instant;

public record StockUpdatedEvent(
        Long productId,
        Long variantId,
        Integer quantityChanged,
        Integer remainingStock,
        String reason,
        Instant occurredAt) {
}
