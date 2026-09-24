package com.fashion.messaging.event;

import java.time.Instant;

public record PaymentCompletedEvent(
        Long orderId,
        Long userId,
        Double amount,
        String paymentMethod,
        Instant occurredAt) {
}
