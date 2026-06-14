package com.fraudwatch.review.config;

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
    public static final String REVIEW_REQUIRED_QUEUE = "fraudwatch.review.transaction-review-required";
    public static final String REVIEW_REQUIRED_ROUTING_KEY = "transaction.review-required";

    public static final String REVIEW_EXCHANGE = "fraudwatch.review.exchange";
    public static final String REVIEW_DECISION_ROUTING_KEY = "review.decision-made";

    @Bean
    DirectExchange fraudExchange() {
        return new DirectExchange(FRAUD_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange reviewExchange() {
        return new DirectExchange(REVIEW_EXCHANGE, true, false);
    }

    @Bean
    Queue reviewRequiredQueue() {
        return new Queue(REVIEW_REQUIRED_QUEUE, true);
    }

    @Bean
    Binding reviewRequiredBinding(DirectExchange fraudExchange, Queue reviewRequiredQueue) {
        return BindingBuilder.bind(reviewRequiredQueue)
            .to(fraudExchange)
            .with(REVIEW_REQUIRED_ROUTING_KEY);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

