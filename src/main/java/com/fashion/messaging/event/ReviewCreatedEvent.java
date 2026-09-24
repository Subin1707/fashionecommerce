package com.fashion.messaging.event;

import java.time.Instant;

public record ReviewCreatedEvent(
        Long reviewId,
        Long userId,
        Long productId,
        Integer rating,
        String status,
        Instant occurredAt) {
}
