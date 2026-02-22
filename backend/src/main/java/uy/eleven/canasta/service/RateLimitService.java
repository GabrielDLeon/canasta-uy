package uy.eleven.canasta.service;

import lombok.RequiredArgsConstructor;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final int MAX_REQUESTS_PER_HOUR = 100;
    private static final String KEY_PREFIX = "rate_limit:";

    public boolean isAllowed(String apiKey) {
        String key = buildKey(apiKey);

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }

        return currentCount <= MAX_REQUESTS_PER_HOUR;
    }

    public long getRemainingRequests(String apiKey) {
        String key = buildKey(apiKey);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return MAX_REQUESTS_PER_HOUR;
        }

        long current = Long.parseLong(value);
        return Math.max(0, MAX_REQUESTS_PER_HOUR - current);
    }

    private String buildKey(String apiKey) {
        long hour = System.currentTimeMillis() / (1000 * 60 * 60);
        return KEY_PREFIX + apiKey + ":" + hour;
    }
}
