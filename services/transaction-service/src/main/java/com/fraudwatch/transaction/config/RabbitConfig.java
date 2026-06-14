package com.fraudwatch.transaction.config;

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
    public static final String TRANSACTION_CREATED_QUEUE = "fraudwatch.transaction.created";
    public static final String TRANSACTION_CREATED_ROUTING_KEY = "transaction.created";

    @Bean
    DirectExchange transactionExchange() {
        return new DirectExchange(TRANSACTION_EXCHANGE, true, false);
    }

    @Bean
    Queue transactionCreatedQueue() {
        return new Queue(TRANSACTION_CREATED_QUEUE, true);
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
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

