package com.fraudwatch.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "fraudwatch.notification.dlx";

    public static final String NOTIFICATION_APPROVED_QUEUE = "fraudwatch.notification.transaction-approved";
    public static final String NOTIFICATION_BLOCKED_QUEUE = "fraudwatch.notification.transaction-blocked";
    public static final String NOTIFICATION_REVIEW_REQUIRED_QUEUE = "fraudwatch.notification.transaction-review-required";
    public static final String NOTIFICATION_REVIEW_DECISION_QUEUE = "fraudwatch.notification.review-decision-made";
    public static final String NOTIFICATION_APPROVED_DLQ = "fraudwatch.notification.transaction-approved.dlq";
    public static final String NOTIFICATION_BLOCKED_DLQ = "fraudwatch.notification.transaction-blocked.dlq";
    public static final String NOTIFICATION_REVIEW_REQUIRED_DLQ = "fraudwatch.notification.transaction-review-required.dlq";
    public static final String NOTIFICATION_REVIEW_DECISION_DLQ = "fraudwatch.notification.review-decision-made.dlq";

    @Bean
    DirectExchange fraudExchange() {
        return new DirectExchange(FRAUD_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue notificationApprovedQueue() {
        return deadLetterEnabledQueue(NOTIFICATION_APPROVED_QUEUE, NOTIFICATION_APPROVED_DLQ);
    }

    @Bean
    Queue notificationBlockedQueue() {
        return deadLetterEnabledQueue(NOTIFICATION_BLOCKED_QUEUE, NOTIFICATION_BLOCKED_DLQ);
    }

    @Bean
    Queue notificationReviewRequiredQueue() {
        return deadLetterEnabledQueue(NOTIFICATION_REVIEW_REQUIRED_QUEUE, NOTIFICATION_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Queue notificationReviewDecisionQueue() {
        return deadLetterEnabledQueue(NOTIFICATION_REVIEW_DECISION_QUEUE, NOTIFICATION_REVIEW_DECISION_DLQ);
    }

    @Bean
    Queue notificationApprovedDeadLetterQueue() {
        return new Queue(NOTIFICATION_APPROVED_DLQ, true);
    }

    @Bean
    Queue notificationBlockedDeadLetterQueue() {
        return new Queue(NOTIFICATION_BLOCKED_DLQ, true);
    }

    @Bean
    Queue notificationReviewRequiredDeadLetterQueue() {
        return new Queue(NOTIFICATION_REVIEW_REQUIRED_DLQ, true);
    }

    @Bean
    Queue notificationReviewDecisionDeadLetterQueue() {
        return new Queue(NOTIFICATION_REVIEW_DECISION_DLQ, true);
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
    Binding notificationApprovedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue notificationApprovedDeadLetterQueue
    ) {
        return BindingBuilder.bind(notificationApprovedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_APPROVED_DLQ);
    }

    @Bean
    Binding notificationBlockedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue notificationBlockedDeadLetterQueue
    ) {
        return BindingBuilder.bind(notificationBlockedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_BLOCKED_DLQ);
    }

    @Bean
    Binding notificationReviewRequiredDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue notificationReviewRequiredDeadLetterQueue
    ) {
        return BindingBuilder.bind(notificationReviewRequiredDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Binding notificationReviewDecisionDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue notificationReviewDecisionDeadLetterQueue
    ) {
        return BindingBuilder.bind(notificationReviewDecisionDeadLetterQueue)
            .to(deadLetterExchange)
            .with(NOTIFICATION_REVIEW_DECISION_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue deadLetterEnabledQueue(String queueName, String deadLetterRoutingKey) {
        return QueueBuilder.durable(queueName)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
            .build();
    }
}
