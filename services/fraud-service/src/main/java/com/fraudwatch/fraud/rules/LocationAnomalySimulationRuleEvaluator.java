package com.fraudwatch.fraud.rules;

import com.fraudwatch.fraud.domain.FraudRule;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class LocationAnomalySimulationRuleEvaluator implements FraudRuleEvaluator {

    static final Duration REGION_TTL = Duration.ofDays(14);

    private final StringRedisTemplate stringRedisTemplate;

    public LocationAnomalySimulationRuleEvaluator(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String supportedRuleCode() {
        return "LOCATION_ANOMALY_SIMULATION";
    }

    @Override
    public Optional<RuleMatch> evaluate(FraudRule rule, RuleContext context) {
        String ipAddress = context.transaction().ipAddress();
        String simulatedRegion = simulatedRegion(ipAddress);
        if (simulatedRegion == null) {
            return Optional.empty();
        }

        String key = "fraud:location:account:%s".formatted(context.transaction().accountId());
        String previousRegion = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.opsForValue().set(key, simulatedRegion, REGION_TTL);

        if (previousRegion != null && !previousRegion.equals(simulatedRegion)) {
            return Optional.of(new RuleMatch(
                rule.getCode(),
                rule.getWeight(),
                "Transaction originated from a simulated new geographic region"
            ));
        }
        return Optional.empty();
    }

    static String simulatedRegion(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        String normalized = ipAddress.trim();
        if (normalized.contains(".")) {
            String[] octets = normalized.split("\\.");
            if (octets.length < 2) {
                return null;
            }
            return "ipv4-region-%s-%s".formatted(octets[0], octets[1]);
        }

        if (normalized.contains(":")) {
            String[] groups = normalized.split(":");
            if (groups.length == 0 || groups[0].isBlank()) {
                return null;
            }
            String secondGroup = groups.length > 1 && !groups[1].isBlank() ? groups[1] : "0";
            return "ipv6-region-%s-%s".formatted(groups[0], secondGroup);
        }

        return null;
    }
}
