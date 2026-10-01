package com.example.JustBuyIt.Services;

import com.example.JustBuyIt.DTOs.UserPrincipalDto;
import com.example.JustBuyIt.Models.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_KEY = "user:";
    private static final String TOKEN_CACHE_KEY = "token:";
    private static final long USER_CACHE_TTL = 5; // minutes
    private static final long TOKEN_CACHE_TTL = 1; // hour

    public void setCacheUser(String username, UserPrincipalDto users) {
        String Key = USER_CACHE_KEY + username;
        redisTemplate.opsForValue().set(Key, users, Duration.ofMinutes(USER_CACHE_TTL));
    }

    public UserPrincipalDto getUserFromCache(String username) {
        String Key = USER_CACHE_KEY + username;
        Object cached = redisTemplate.opsForValue().get(Key);
        if (cached instanceof UserPrincipalDto) {
            return (UserPrincipalDto) cached;
        }
        return null;
    }

    public UserPrincipalDto getUserDetailsFromCache(String username) {
        return getUserFromCache(username);
    }

    public void setCacheToken(String token, String username) {
        String Key = TOKEN_CACHE_KEY + token;
        redisTemplate.opsForValue().set(Key, username, Duration.ofHours(TOKEN_CACHE_TTL));
    }

    public String getUserNameFromTokenCache(String token) {
        Object v = redisTemplate.opsForValue().get(TOKEN_CACHE_KEY + token);
        return v instanceof String s ? s : null;
    }

    public void invalidateUserCache(String username) {
        String Key = USER_CACHE_KEY + username;
        redisTemplate.delete(Key);
    }

    public void invalidateTokenCache(String token) {
        String Key = TOKEN_CACHE_KEY + token;
        redisTemplate.delete(Key);
    }

    public void invalidateAllCaches() {
        redisTemplate.delete(redisTemplate.keys(USER_CACHE_KEY + "*"));
    }
}
