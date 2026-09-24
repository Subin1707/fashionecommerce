package com.fashion.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EVENTS_EXCHANGE = "fashion.events";
    public static final String STOCK_QUEUE = "fashion.stock.events";
    public static final String LOYALTY_QUEUE = "fashion.loyalty.events";
    public static final String NOTIFICATION_QUEUE = "fashion.notification.events";

    @Bean
    public TopicExchange fashionEventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockEventsQueue() {
        return new Queue(STOCK_QUEUE, true);
    }

    @Bean
    public Queue loyaltyEventsQueue() {
        return new Queue(LOYALTY_QUEUE, true);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding stockOrderBinding(Queue stockEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(stockEventsQueue).to(fashionEventsExchange).with("OrderCreated");
    }

    @Bean
    public Binding stockUpdatedBinding(Queue stockEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(stockEventsQueue).to(fashionEventsExchange).with("StockUpdated");
    }

    @Bean
    public Binding loyaltyOrderBinding(Queue loyaltyEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(loyaltyEventsQueue).to(fashionEventsExchange).with("OrderCreated");
    }

    @Bean
    public Binding loyaltyPaymentBinding(Queue loyaltyEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(loyaltyEventsQueue).to(fashionEventsExchange).with("PaymentCompleted");
    }

    @Bean
    public Binding notificationOrderBinding(Queue notificationEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(fashionEventsExchange).with("OrderCreated");
    }

    @Bean
    public Binding notificationPaymentBinding(Queue notificationEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(fashionEventsExchange).with("PaymentCompleted");
    }

    @Bean
    public Binding notificationReviewBinding(Queue notificationEventsQueue, TopicExchange fashionEventsExchange) {
        return BindingBuilder.bind(notificationEventsQueue).to(fashionEventsExchange).with("ReviewCreated");
    }
}
