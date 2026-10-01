package com.example.JustBuyIt.Configurations;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class LayeredCache implements Cache {

    private final String cacheName;
    private final @Nullable Cache l1;   // Caffeine
    private final @Nullable Cache l2;   // Redis

    public LayeredCache(String cacheName, @Nullable Cache l1, @Nullable Cache l2) {
        this.cacheName = cacheName;
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return this.cacheName;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {
        ValueWrapper v1 = l1 == null ? null : l1.get(key);
        if (v1 != null) return v1;

        ValueWrapper v2 = l2 == null ? null : l2.get(key);
        if (v2 != null) {
            Object value = v2.get();
            if (value != null && l1 != null) {
                l1.put(key, value);
            }
            return v2;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(Object key, @Nullable Class<T> type) {
        T v1 = l1 == null ? null : l1.get(key, type);
        if (v1 != null) return v1;

        T v2 = l2 == null ? null : l2.get(key, type);
        if (v2 != null) {
            if (l1 != null) {
                l1.put(key, v2);
            }
            return v2;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(Object key, Callable<T> valueLoader) {
        if (l1 != null) {
            ValueWrapper v1 = l1.get(key);
            if (v1 != null) {
                return (T) v1.get();
            }
        }

        if (l2 != null) {
            T value;
            try {
                value = l2.get(key, valueLoader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (value != null && l1 != null) {
                l1.put(key, value);
            }
            return value;
        }

        // Fallback behavior if both cache stores are unavailable
        try {
            return valueLoader.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        if (value == null) return;
        if (l1 != null) l1.put(key, value);
        if (l2 != null) l2.put(key, value);
    }

    @Override
    public void evict(Object key) {
        if (l1 != null) l1.evict(key);
        if (l2 != null) l2.evict(key);
    }

    @Override
    public void clear() {
        if (l1 != null) l1.clear();
        if (l2 != null) l2.clear();
    }
}
