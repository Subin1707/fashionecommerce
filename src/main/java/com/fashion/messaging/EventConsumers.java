package com.fashion.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventConsumers {

    @RabbitListener(queues = RabbitMqConfig.STOCK_QUEUE)
    public void handleStockEvent(String event) {
        log.info("Stock service received event: {}", event);
    }

    @RabbitListener(queues = RabbitMqConfig.LOYALTY_QUEUE)
    public void handleLoyaltyEvent(String event) {
        log.info("Loyalty service received event: {}", event);
    }

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void handleNotificationEvent(String event) {
        log.info("Notification service received event: {}", event);
    }
}
