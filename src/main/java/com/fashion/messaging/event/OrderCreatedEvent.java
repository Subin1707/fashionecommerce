package com.fashion.messaging.event;

import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        Double totalAmount,
        Double finalAmount,
        String paymentMethod,
        List<OrderItemEvent> items,
        Instant occurredAt) {
}
