package com.fraudwatch.transaction.config;

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

    public static final String TRANSACTION_EXCHANGE = "fraudwatch.transaction.exchange";
    public static final String TRANSACTION_CREATED_QUEUE = "fraudwatch.transaction.created";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";
    public static final String TRANSACTION_STATUS_CHANGED_ROUTING_KEY = "transaction.status-changed";

    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "fraudwatch.transaction.dlx";
    public static final String FRAUD_APPROVED_QUEUE = "fraudwatch.transaction.fraud-approved";
    public static final String FRAUD_BLOCKED_QUEUE = "fraudwatch.transaction.fraud-blocked";
    public static final String FRAUD_REVIEW_REQUIRED_QUEUE = "fraudwatch.transaction.fraud-review-required";
    public static final String REVIEW_DECISION_QUEUE = "fraudwatch.transaction.review-decision-made";
    public static final String FRAUD_APPROVED_DLQ = "fraudwatch.transaction.fraud-approved.dlq";
    public static final String FRAUD_BLOCKED_DLQ = "fraudwatch.transaction.fraud-blocked.dlq";
    public static final String FRAUD_REVIEW_REQUIRED_DLQ = "fraudwatch.transaction.fraud-review-required.dlq";
    public static final String REVIEW_DECISION_DLQ = "fraudwatch.transaction.review-decision-made.dlq";

    @Bean
    DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE, true, false);
    }

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
    Queue transactionCreatedQueue() {
        return new Queue(TRANSACTION_CREATED_QUEUE, true);
    }

    @Bean
    Queue fraudApprovedQueue() {
        return deadLetterEnabledQueue(FRAUD_APPROVED_QUEUE, FRAUD_APPROVED_DLQ);
    }

    @Bean
    Queue fraudBlockedQueue() {
        return deadLetterEnabledQueue(FRAUD_BLOCKED_QUEUE, FRAUD_BLOCKED_DLQ);
    }

    @Bean
    Queue fraudReviewRequiredQueue() {
        return deadLetterEnabledQueue(FRAUD_REVIEW_REQUIRED_QUEUE, FRAUD_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Queue reviewDecisionQueue() {
        return deadLetterEnabledQueue(REVIEW_DECISION_QUEUE, REVIEW_DECISION_DLQ);
    }

    @Bean
    Queue fraudApprovedDeadLetterQueue() {
        return new Queue(FRAUD_APPROVED_DLQ, true);
    }

    @Bean
    Queue fraudBlockedDeadLetterQueue() {
        return new Queue(FRAUD_BLOCKED_DLQ, true);
    }

    @Bean
    Queue fraudReviewRequiredDeadLetterQueue() {
        return new Queue(FRAUD_REVIEW_REQUIRED_DLQ, true);
    }

    @Bean
    Queue reviewDecisionDeadLetterQueue() {
        return new Queue(REVIEW_DECISION_DLQ, true);
    }

    @Bean
    Binding transactionCreatedBinding(
        DirectExchange transactionExchange,
        Queue transactionCreatedQueue
    ) {
        return BindingBuilder.bind(transactionCreatedQueue)
            .to(transactionExchange)
            .with(TRANSACTION_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding fraudApprovedBinding(DirectExchange fraudExchange, Queue fraudApprovedQueue) {
        return BindingBuilder.bind(fraudApprovedQueue)
            .to(fraudExchange)
            .with("transaction.approved");
    }

    @Bean
    Binding fraudBlockedBinding(DirectExchange fraudExchange, Queue fraudBlockedQueue) {
        return BindingBuilder.bind(fraudBlockedQueue)
            .to(fraudExchange)
            .with("transaction.blocked");
    }

    @Bean
    Binding fraudReviewRequiredBinding(DirectExchange fraudExchange, Queue fraudReviewRequiredQueue) {
        return BindingBuilder.bind(fraudReviewRequiredQueue)
            .to(fraudExchange)
            .with("transaction.review-required");
    }

    @Bean
    Binding reviewDecisionBinding(DirectExchange reviewExchange, Queue reviewDecisionQueue) {
        return BindingBuilder.bind(reviewDecisionQueue)
            .to(reviewExchange)
            .with("review.decision-made");
    }

    @Bean
    Binding fraudApprovedDeadLetterBinding(DirectExchange deadLetterExchange, Queue fraudApprovedDeadLetterQueue) {
        return BindingBuilder.bind(fraudApprovedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(FRAUD_APPROVED_DLQ);
    }

    @Bean
    Binding fraudBlockedDeadLetterBinding(DirectExchange deadLetterExchange, Queue fraudBlockedDeadLetterQueue) {
        return BindingBuilder.bind(fraudBlockedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(FRAUD_BLOCKED_DLQ);
    }

    @Bean
    Binding fraudReviewRequiredDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue fraudReviewRequiredDeadLetterQueue
    ) {
        return BindingBuilder.bind(fraudReviewRequiredDeadLetterQueue)
            .to(deadLetterExchange)
            .with(FRAUD_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Binding reviewDecisionDeadLetterBinding(DirectExchange deadLetterExchange, Queue reviewDecisionDeadLetterQueue) {
        return BindingBuilder.bind(reviewDecisionDeadLetterQueue)
            .to(deadLetterExchange)
            .with(REVIEW_DECISION_DLQ);
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
