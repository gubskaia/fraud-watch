package com.fraudwatch.audit.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String TRANSACTION_EXCHANGE = "fraudwatch.transaction.exchange";
    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";

    public static final String AUDIT_TRANSACTION_CREATED_QUEUE = "fraudwatch.audit.transaction-created";
    public static final String AUDIT_TRANSACTION_APPROVED_QUEUE = "fraudwatch.audit.transaction-approved";
    public static final String AUDIT_TRANSACTION_BLOCKED_QUEUE = "fraudwatch.audit.transaction-blocked";
    public static final String AUDIT_TRANSACTION_REVIEW_REQUIRED_QUEUE = "fraudwatch.audit.transaction-review-required";
    public static final String AUDIT_REVIEW_DECISION_QUEUE = "fraudwatch.audit.review-decision-made";

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
    Queue auditTransactionCreatedQueue() {
        return new Queue(AUDIT_TRANSACTION_CREATED_QUEUE, true);
    }

    @Bean
    Queue auditTransactionApprovedQueue() {
        return new Queue(AUDIT_TRANSACTION_APPROVED_QUEUE, true);
    }

    @Bean
    Queue auditTransactionBlockedQueue() {
        return new Queue(AUDIT_TRANSACTION_BLOCKED_QUEUE, true);
    }

    @Bean
    Queue auditTransactionReviewRequiredQueue() {
        return new Queue(AUDIT_TRANSACTION_REVIEW_REQUIRED_QUEUE, true);
    }

    @Bean
    Queue auditReviewDecisionQueue() {
        return new Queue(AUDIT_REVIEW_DECISION_QUEUE, true);
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
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

