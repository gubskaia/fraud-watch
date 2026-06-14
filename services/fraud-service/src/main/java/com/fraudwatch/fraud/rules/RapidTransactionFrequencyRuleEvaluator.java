package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RapidTransactionFrequencyRuleEvaluator implements FraudRuleEvaluator {

    private static final long WINDOW_SECONDS = 300;
    private static final long THRESHOLD_COUNT = 3;

    private final StringRedisTemplate stringRedisTemplate;

    public RapidTransactionFrequencyRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "RAPID_TRANSACTION_FREQUENCY";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String key = "fraud:velocity:account:%s".formatted(context.transaction().accountId());
        Long currentCount = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));

        if (currentCount != null && currentCount > THRESHOLD_COUNT) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Account exceeded rapid transaction frequency threshold"
            ));
        }
        return Optional.empty();
    }
}

