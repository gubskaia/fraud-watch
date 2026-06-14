package com.fraudwatch.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";

    public static final String NOTIFICATION_APPROVED_QUEUE = "fraudwatch.notification.transaction-approved";
    public static final String NOTIFICATION_BLOCKED_QUEUE = "fraudwatch.notification.transaction-blocked";
    public static final String NOTIFICATION_REVIEW_REQUIRED_QUEUE = "fraudwatch.notification.transaction-review-required";
    public static final String NOTIFICATION_REVIEW_DECISION_QUEUE = "fraudwatch.notification.review-decision-made";

    @Bean
    DirectExchange fraudExchange() {
        return new DirectExchange(FRAUD_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationApprovedQueue() {
        return new Queue(NOTIFICATION_APPROVED_QUEUE, true);
    }

    @Bean
    Queue notificationBlockedQueue() {
        return new Queue(NOTIFICATION_BLOCKED_QUEUE, true);
    }

    @Bean
    Queue notificationReviewRequiredQueue() {
        return new Queue(NOTIFICATION_REVIEW_REQUIRED_QUEUE, true);
    }

    @Bean
    Queue notificationReviewDecisionQueue() {
        return new Queue(NOTIFICATION_REVIEW_DECISION_QUEUE, true);
    }

    @Bean
    Binding notificationApprovedBinding(DirectExchange fraudExchange, Queue notificationApprovedQueue) {
        return BindingBuilder.bind(notificationApprovedQueue)
            .to(fraudExchange)
            .with("transaction.approved");
    }

    @Bean
    Binding notificationBlockedBinding(DirectExchange fraudExchange, Queue notificationBlockedQueue) {
        return BindingBuilder.bind(notificationBlockedQueue)
            .to(fraudExchange)
            .with("transaction.blocked");
    }

    @Bean
    Binding notificationReviewRequiredBinding(DirectExchange fraudExchange, Queue notificationReviewRequiredQueue) {
        return BindingBuilder.bind(notificationReviewRequiredQueue)
            .to(fraudExchange)
            .with("transaction.review-required");
    }

    @Bean
    Binding notificationReviewDecisionBinding(DirectExchange reviewExchange, Queue notificationReviewDecisionQueue) {
        return BindingBuilder.bind(notificationReviewDecisionQueue)
            .to(reviewExchange)
            .with("review.decision-made");
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

