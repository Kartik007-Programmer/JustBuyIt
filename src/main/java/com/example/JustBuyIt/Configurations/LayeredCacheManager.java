package com.example.JustBuyIt.Configurations;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
    public @Nullable Cache getCache(@NonNull String name) {
        return caches.computeIfAbsent(name, n -> {
            Cache c1 = l1.getCache(n);
            Cache c2 = l2.getCache(n);
            if (c1 == null && c2 == null) {
                return null;
            }
            return new LayeredCache(n,c1,c2);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        Set<String> names = new HashSet<>();
        if (l1.getCacheNames() != null) {
            names.addAll(l1.getCacheNames());
        }
        if (l2.getCacheNames() != null) {
            names.addAll(l2.getCacheNames());
        }
        return names;
    }


}
