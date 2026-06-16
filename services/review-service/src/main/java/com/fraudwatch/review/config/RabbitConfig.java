package com.fraudwatch.review.config;

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
    public static final String DEAD_LETTER_EXCHANGE = "fraudwatch.review.dlx";
    public static final String REVIEW_REQUIRED_QUEUE = "fraudwatch.review.transaction-review-required";
    public static final String REVIEW_REQUIRED_ROUTING_KEY = "transaction.review-required";
    public static final String REVIEW_REQUIRED_DLQ = "fraudwatch.review.transaction-review-required.dlq";

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
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue reviewRequiredQueue() {
        return QueueBuilder.durable(REVIEW_REQUIRED_QUEUE)
            .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", REVIEW_REQUIRED_DLQ)
            .build();
    }

    @Bean
    Queue reviewRequiredDeadLetterQueue() {
        return new Queue(REVIEW_REQUIRED_DLQ, true);
    }

    @Bean
    Binding reviewRequiredBinding(DirectExchange fraudExchange, Queue reviewRequiredQueue) {
        return BindingBuilder.bind(reviewRequiredQueue)
            .to(fraudExchange)
            .with(REVIEW_REQUIRED_ROUTING_KEY);
    }

    @Bean
    Binding reviewRequiredDeadLetterBinding(
        DirectExchange deadLetterExchange,
        Queue reviewRequiredDeadLetterQueue
    ) {
        return BindingBuilder.bind(reviewRequiredDeadLetterQueue)
            .to(deadLetterExchange)
            .with(REVIEW_REQUIRED_DLQ);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
