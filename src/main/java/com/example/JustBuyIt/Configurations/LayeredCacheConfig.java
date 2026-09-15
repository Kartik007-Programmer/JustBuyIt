package com.example.JustBuyIt.Configurations;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

@Configuration
public class LayeredCacheConfig {

    @Primary
    @Bean
    public CacheManager layeredCacheManager(RedisCacheManager redisCacheManager) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(2))
                .recordStats());

        caffeineCacheManager.setCacheNames(java.util.List.of("user", "products", "cart"));

        return new LayeredCacheManager(caffeineCacheManager,redisCacheManager);
    }
}
