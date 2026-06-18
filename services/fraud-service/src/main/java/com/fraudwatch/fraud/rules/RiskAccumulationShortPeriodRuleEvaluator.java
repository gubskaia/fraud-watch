package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RiskAccumulationShortPeriodRuleEvaluator implements FraudRuleEvaluator {

    static final long WINDOW_SECONDS = 600;
    static final BigDecimal TOTAL_AMOUNT_THRESHOLD = new BigDecimal("25000.00");

    private final StringRedisTemplate stringRedisTemplate;

    public RiskAccumulationShortPeriodRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "RISK_ACCUMULATION_SHORT_PERIOD";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        BigDecimal amount = context.transaction().amount();
        if (amount == null) {
            return Optional.empty();
        }

        String key = "fraud:amount:account:%s".formatted(context.transaction().accountId());
        long amountInMinorUnits = amount.movePointRight(2).longValue();
        Long totalMinorUnits = stringRedisTemplate.opsForValue().increment(key, amountInMinorUnits);
        stringRedisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));

        if (totalMinorUnits != null && totalMinorUnits >= TOTAL_AMOUNT_THRESHOLD.movePointRight(2).longValue()) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Account accumulated a high transaction amount in a short period"
            ));
        }
        return Optional.empty();
    }
}
