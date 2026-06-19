package com.fraudwatch.fraud.config;

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
    public static final String DEAD_LETTER_EXCHANGE = "fraudwatch.fraud.dlx";
    public static final String TRANSACTION_CREATED_QUEUE = "fraudwatch.fraud.transaction-created";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";
    public static final String TRANSACTION_CREATED_DLQ = "fraudwatch.fraud.transaction-created.dlq";
    public static final String TRANSACTION_STATUS_CHANGED_QUEUE = "fraudwatch.fraud.transaction-status-changed";
    public static final String TRANSACTION_STATUS_CHANGED_ROUTING_KEY = "transaction.status-changed";
    public static final String TRANSACTION_STATUS_CHANGED_DLQ = "fraudwatch.fraud.transaction-status-changed.dlq";

    public static final String FRAUD_EXCHANGE = "fraudwatch.fraud.exchange";
    public static final String APPROVED_ROUTING_KEY = "transaction.approved";
    public static final String BLOCKED_ROUTING_KEY = "transaction.blocked";
    public static final String REVIEW_REQUIRED_ROUTING_KEY = "transaction.review-required";

    @Bean
    DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange fraudExchange() {
        return new DirectExchange(FRAUD_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue transactionCreatedQueue() {
        return QueueBuilder.durable(TRANSACTION_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", TRANSACTION_CREATED_DLQ)
            .build();
    }

    @Bean
    Queue transactionCreatedDeadLetterQueue() {
        return new Queue(TRANSACTION_CREATED_DLQ, true);
    }

    @Bean
    Queue transactionStatusChangedQueue() {
        return QueueBuilder.durable(TRANSACTION_STATUS_CHANGED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", TRANSACTION_STATUS_CHANGED_DLQ)
            .build();
    }

    @Bean
    Queue transactionStatusChangedDeadLetterQueue() {
        return new Queue(TRANSACTION_STATUS_CHANGED_DLQ, true);
    }

    @Bean
    Binding transactionCreatedBinding(DirectExchange transactionExchange, Queue transactionCreatedQueue) {
        return BindingBuilder.bind(transactionCreatedQueue)
            .to(transactionExchange)
            .with(TRANSACTION_CREATED_ROUTING_KEY);
    }

    @Bean
    Binding transactionCreatedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue transactionCreatedDeadLetterQueue
    ) {
        return BindingBuilder.bind(transactionCreatedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(TRANSACTION_CREATED_DLQ);
    }

    @Bean
    Binding transactionStatusChangedBinding(DirectExchange transactionExchange, Queue transactionStatusChangedQueue) {
        return BindingBuilder.bind(transactionStatusChangedQueue)
            .to(transactionExchange)
            .with(TRANSACTION_STATUS_CHANGED_ROUTING_KEY);
    }

    @Bean
    Binding transactionStatusChangedDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue transactionStatusChangedDeadLetterQueue
    ) {
        return BindingBuilder.bind(transactionStatusChangedDeadLetterQueue)
            .to(deadLetterExchange)
            .with(TRANSACTION_STATUS_CHANGED_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
