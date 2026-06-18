package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HighRiskAccountBehaviorRuleEvaluator implements FraudRuleEvaluator {

    private static final Duration WINDOW = Duration.ofHours(24);
    private static final long RISK_SIGNAL_THRESHOLD = 4;
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("10000.00");
    private static final Set<String> HIGH_RISK_CATEGORIES = Set.of(
        "CRYPTO",
        "GAMBLING",
        "GIFT_CARDS",
        "MONEY_TRANSFER"
    );

    private final StringRedisTemplate stringRedisTemplate;

    public HighRiskAccountBehaviorRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "HIGH_RISK_ACCOUNT_BEHAVIOR";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        long currentRiskSignals = riskSignals(context);
        if (currentRiskSignals == 0) {
            return Optional.empty();
        }

        String key = "fraud:behavior:account:%s".formatted(context.transaction().accountId());
        Long accumulatedSignals = stringRedisTemplate.opsForValue().increment(key, currentRiskSignals);
        stringRedisTemplate.expire(key, WINDOW);

        if (accumulatedSignals != null && accumulatedSignals >= RISK_SIGNAL_THRESHOLD) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Account accumulated multiple high-risk behavior signals in a short period"
            ));
        }
        return Optional.empty();
    }

    static long riskSignals(RuleContext context) {
        long signals = 0;

        if (context.transaction().amount() != null
            && context.transaction().amount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0) {
            signals++;
        }

        String merchantCategory = context.transaction().merchantCategory();
        if (merchantCategory != null && HIGH_RISK_CATEGORIES.contains(merchantCategory.trim().toUpperCase())) {
            signals++;
        }

        String ipAddress = context.transaction().ipAddress();
        if (LocationAnomalySimulationRuleEvaluator.simulatedRegion(ipAddress) != null
            && ipAddress != null
            && !ipAddress.isBlank()
            && !ipAddress.startsWith("127.")
            && !ipAddress.startsWith("10.")
            && !ipAddress.startsWith("192.168.")) {
            signals++;
        }

        int hour = context.transaction().createdAt().atZone(ZoneOffset.UTC).getHour();
        if (hour < 5) {
            signals++;
        }

        return signals;
    }
}
