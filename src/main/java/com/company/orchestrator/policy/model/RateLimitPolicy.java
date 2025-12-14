package com.company.orchestrator.policy.model;

import com.company.orchestrator.api.dto.TransferRequestDto;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting policy using Redis for distributed rate limiting
 * Uses Redis Sorted Sets (ZSET) to track request timestamps per consumer
 * Automatically evicts old entries using TTL (1 hour)
 */
public class RateLimitPolicy implements Policy {

    private static final String KEY_PREFIX = "rate_limit:consumer:";
    private static final long TTL_HOURS = 1L;

    private final int maxRequests;
    private final Duration window;
    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitPolicy(int maxRequestsPerHour, RedisTemplate<String, Object> redisTemplate) {
        this.maxRequests = maxRequestsPerHour;
        this.window = Duration.ofHours(1);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public PolicyEvaluationResult evaluate(TransferRequestDto request) {
        String consumerId = request.getConsumerId();
        // Use current time for rate limit evaluation
        Instant now = Instant.now();

        String key = KEY_PREFIX + consumerId;
        long nowMillis = now.toEpochMilli();
        long windowStartMillis = now.minus(window).toEpochMilli();

        try {
            // Remove timestamps older than the window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStartMillis);

            // Count current requests in the window
            Long currentCount = redisTemplate.opsForZSet().count(key, windowStartMillis, nowMillis);
            boolean allowed = currentCount != null && currentCount < maxRequests;

            if (allowed) {
                // Add current request timestamp
                redisTemplate.opsForZSet().add(key, String.valueOf(nowMillis), nowMillis);

                // Set TTL to auto-evict after 1 hour
                redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
            }

            return new PolicyEvaluationResult(this, allowed,
                    allowed ? null : "Rate limit exceeded for consumer " + consumerId +
                            " (current: " + currentCount + ", max: " + maxRequests + ")");

        } catch (Exception ex) {
            // On Redis failure, fail-open (allow the request)
            return new PolicyEvaluationResult(this, true, null);
        }
    }
}
