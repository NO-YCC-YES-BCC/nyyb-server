package com.nyyb.nyybserver.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration expiration) {
        redisTemplate.opsForValue().set(
                generateKey(userId),
                refreshToken,
                expiration
        );
    }

    public String get(Long userId) {
        return redisTemplate.opsForValue()
                .get(generateKey(userId));
    }

    public void delete(Long userId) {
        redisTemplate.delete(generateKey(userId));
    }

    private String generateKey(Long userId) {
        return "refresh:" + userId;
    }
}
