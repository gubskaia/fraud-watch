package com.fraudwatch.fraud.service;

import com.fraudwatch.events.transaction.TransactionStatusChangedPayload;
import com.fraudwatch.fraud.rules.RepeatedFailedAttemptsRuleEvaluator;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailedAttemptTrackingService {

    private static final Duration WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    public FailedAttemptTrackingService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public void recordStatusChange(TransactionStatusChangedPayload payload) {
        if (!"BLOCKED".equals(payload.newStatus()) || payload.accountId() == null) {
            return;
        }

        String key = RepeatedFailedAttemptsRuleEvaluator.failedAttemptKey(payload.accountId());
        stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, WINDOW);
    }
}
