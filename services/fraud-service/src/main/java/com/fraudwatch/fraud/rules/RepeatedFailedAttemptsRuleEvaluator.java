package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RepeatedFailedAttemptsRuleEvaluator implements FraudRuleEvaluator {

    static final long THRESHOLD_COUNT = 2;

    private final StringRedisTemplate stringRedisTemplate;

    public RepeatedFailedAttemptsRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "REPEATED_FAILED_ATTEMPTS";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String key = failedAttemptKey(context.transaction().accountId());
        String rawCount = stringRedisTemplate.opsForValue().get(key);
        long blockedAttempts = parseCount(rawCount);

        if (blockedAttempts >= THRESHOLD_COUNT) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Account has repeated recently blocked transactions"
            ));
        }
        return Optional.empty();
    }

    public static String failedAttemptKey(Long accountId) {
        return "fraud:failed-attempts:account:%s".formatted(accountId);
    }

    private long parseCount(String rawCount) {
        if (rawCount == null || rawCount.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(rawCount.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
