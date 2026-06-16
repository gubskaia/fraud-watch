package com.fraudwatch.audit.config;

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
    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";
    public static final String DEAD_LETTER_EXCHANGE = "fraudwatch.audit.dlx";

    public static final String AUDIT_TRANSACTION_CREATED_QUEUE = "fraudwatch.audit.transaction-created";
    public static final String AUDIT_TRANSACTION_APPROVED_QUEUE = "fraudwatch.audit.transaction-approved";
    public static final String AUDIT_TRANSACTION_BLOCKED_QUEUE = "fraudwatch.audit.transaction-blocked";
    public static final String AUDIT_TRANSACTION_REVIEW_REQUIRED_QUEUE = "fraudwatch.audit.transaction-review-required";
    public static final String AUDIT_REVIEW_DECISION_QUEUE = "fraudwatch.audit.review-decision-made";
    public static final String AUDIT_TRANSACTION_CREATED_DLQ = "fraudwatch.audit.transaction-created.dlq";
    public static final String AUDIT_TRANSACTION_APPROVED_DLQ = "fraudwatch.audit.transaction-approved.dlq";
    public static final String AUDIT_TRANSACTION_BLOCKED_DLQ = "fraudwatch.audit.transaction-blocked.dlq";
    public static final String AUDIT_TRANSACTION_REVIEW_REQUIRED_DLQ = "fraudwatch.audit.transaction-review-required.dlq";
    public static final String AUDIT_REVIEW_DECISION_DLQ = "fraudwatch.audit.review-decision-made.dlq";

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
    Queue auditTransactionCreatedQueue() {
        return deadLetterEnabledQueue(AUDIT_TRANSACTION_CREATED_QUEUE, AUDIT_TRANSACTION_CREATED_DLQ);
    }

    @Bean
    Queue auditTransactionApprovedQueue() {
        return deadLetterEnabledQueue(AUDIT_TRANSACTION_APPROVED_QUEUE, AUDIT_TRANSACTION_APPROVED_DLQ);
    }

    @Bean
    Queue auditTransactionBlockedQueue() {
        return deadLetterEnabledQueue(AUDIT_TRANSACTION_BLOCKED_QUEUE, AUDIT_TRANSACTION_BLOCKED_DLQ);
    }

    @Bean
    Queue auditTransactionReviewRequiredQueue() {
        return deadLetterEnabledQueue(AUDIT_TRANSACTION_REVIEW_REQUIRED_QUEUE, AUDIT_TRANSACTION_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Queue auditReviewDecisionQueue() {
        return deadLetterEnabledQueue(AUDIT_REVIEW_DECISION_QUEUE, AUDIT_REVIEW_DECISION_DLQ);
    }

    @Bean
    Queue auditTransactionCreatedDeadLetterQueue() {
        return new Queue(AUDIT_TRANSACTION_CREATED_DLQ, true);
    }

    @Bean
    Queue auditTransactionApprovedDeadLetterQueue() {
        return new Queue(AUDIT_TRANSACTION_APPROVED_DLQ, true);
    }

    @Bean
    Queue auditTransactionBlockedDeadLetterQueue() {
        return new Queue(AUDIT_TRANSACTION_BLOCKED_DLQ, true);
    }

    @Bean
    Queue auditTransactionReviewRequiredDeadLetterQueue() {
        return new Queue(AUDIT_TRANSACTION_REVIEW_REQUIRED_DLQ, true);
    }

    @Bean
    Queue auditReviewDecisionDeadLetterQueue() {
        return new Queue(AUDIT_REVIEW_DECISION_DLQ, true);
    }

    @Bean
    Binding auditTransactionCreatedBinding(DirectExchange transactionExchange, Queue auditTransactionCreatedQueue) {
        return BindingBuilder.bind(auditTransactionCreatedQueue)
            .to(transactionExchange)
            .with("transaction.created");
    }

    @Bean
    Binding auditTransactionApprovedBinding(DirectExchange fraudExchange, Queue auditTransactionApprovedQueue) {
        return BindingBuilder.bind(auditTransactionApprovedQueue)
            .to(fraudExchange)
            .with("transaction.approved");
    }

    @Bean
    Binding auditTransactionBlockedBinding(DirectExchange fraudExchange, Queue auditTransactionBlockedQueue) {
        return BindingBuilder.bind(auditTransactionBlockedQueue)
            .to(fraudExchange)
            .with("transaction.blocked");
    }

    @Bean
    Binding auditTransactionReviewRequiredBinding(DirectExchange fraudExchange, Queue auditTransactionReviewRequiredQueue) {
        return BindingBuilder.bind(auditTransactionReviewRequiredQueue)
            .to(fraudExchange)
            .with("transaction.review-required");
    }

    @Bean
    Binding auditReviewDecisionBinding(DirectExchange reviewExchange, Queue auditReviewDecisionQueue) {
        return BindingBuilder.bind(auditReviewDecisionQueue)
            .to(reviewExchange)
            .with("review.decision-made");
    }

    @Bean
    Binding auditTransactionCreatedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue auditTransactionCreatedDeadLetterQueue
    ) {
        return BindingBuilder.bind(auditTransactionCreatedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(AUDIT_TRANSACTION_CREATED_DLQ);
    }

    @Bean
    Binding auditTransactionApprovedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue auditTransactionApprovedDeadLetterQueue
    ) {
        return BindingBuilder.bind(auditTransactionApprovedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(AUDIT_TRANSACTION_APPROVED_DLQ);
    }

    @Bean
    Binding auditTransactionBlockedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue auditTransactionBlockedDeadLetterQueue
    ) {
        return BindingBuilder.bind(auditTransactionBlockedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(AUDIT_TRANSACTION_BLOCKED_DLQ);
    }

    @Bean
    Binding auditTransactionReviewRequiredDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue auditTransactionReviewRequiredDeadLetterQueue
    ) {
        return BindingBuilder.bind(auditTransactionReviewRequiredDeadLetterQueue)
            .to(deadLetterExchange)
            .with(AUDIT_TRANSACTION_REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Binding auditReviewDecisionDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue auditReviewDecisionDeadLetterQueue
    ) {
        return BindingBuilder.bind(auditReviewDecisionDeadLetterQueue)
            .to(deadLetterExchange)
            .with(AUDIT_REVIEW_DECISION_DLQ);
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
