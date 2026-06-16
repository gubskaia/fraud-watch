package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewIpDetectionRuleEvaluator implements FraudRuleEvaluator {

    private final StringRedisTemplate stringRedisTemplate;

    public NewIpDetectionRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "NEW_IP_DETECTION";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String ipAddress = context.transaction().ipAddress();
        if (ipAddress == null || ipAddress.isBlank()) {
            return Optional.empty();
        }

        String normalizedIp = ipAddress.trim();
        String key = "fraud:ip:account:%s:%s".formatted(context.transaction().accountId(), normalizedIp);
        Boolean existed = stringRedisTemplate.hasKey(key);
        stringRedisTemplate.opsForValue().set(key, "seen", Duration.ofDays(30));

        if (Boolean.FALSE.equals(existed)) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction originates from a previously unseen IP address"
            ));
        }
        return Optional.empty();
    }
}
