package com.example.JustBuyIt.Configurations;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LayeredCacheManager implements CacheManager {

    private final CacheManager l1;   // Caffeine
    private final CacheManager l2;   // Redis
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public LayeredCacheManager(CacheManager l1, CacheManager l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public @Nullable Cache getCache(String name) {
        return caches.computeIfAbsent(name, n -> {
            Cache c1 = l1.getCache(n);
            Cache c2 = l2.getCache(n);
            if (c1 == null && c2 == null) {return null;}
            if (c1 == null) {return c2;}
            if (c2 == null) {return c1;}
            return new LayeredCache(c1,c2);
        } );
    }

    @Override
    public Collection<String> getCacheNames() {
        return l1.getCacheNames();
    }
}
