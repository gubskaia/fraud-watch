package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NewDeviceDetectionRuleEvaluator implements FraudRuleEvaluator {

    private final StringRedisTemplate stringRedisTemplate;

    public NewDeviceDetectionRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "NEW_DEVICE_DETECTION";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String deviceId = context.transaction().deviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return Optional.empty();
        }

        String key = "fraud:device:account:%s:%s".formatted(context.transaction().accountId(), deviceId.trim());
        Boolean existed = stringRedisTemplate.hasKey(key);
        stringRedisTemplate.opsForValue().set(key, "seen", Duration.ofDays(30));

        if (Boolean.FALSE.equals(existed)) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction originates from a previously unseen device"
            ));
        }
        return Optional.empty();
    }
}

