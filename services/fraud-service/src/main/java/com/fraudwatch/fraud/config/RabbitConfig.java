package com.fraudwatch.fraud.config;

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
    public static final String TRANSACTION_CREATED_QUEUE = "fraudwatch.fraud.transaction-created";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";

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
    Queue transactionCreatedQueue() {
        return new Queue(TRANSACTION_CREATED_QUEUE, true);
    }

    @Bean
    Binding transactionCreatedBinding(DirectExchange transactionExchange, Queue transactionCreatedQueue) {
        return BindingBuilder.bind(transactionCreatedQueue)
            .to(transactionExchange)
            .with(TRANSACTION_CREATED_ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

