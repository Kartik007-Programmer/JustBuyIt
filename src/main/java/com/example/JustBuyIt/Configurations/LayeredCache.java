package com.example.JustBuyIt.Configurations;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class LayeredCache implements Cache {

    private final Cache l1;   // Caffeine
    private final Cache l2;   // Redis

    public LayeredCache(Cache l1, Cache l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    @Override
    public String getName() {
        return l1.getName();
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {

        ValueWrapper v1 = l1.get(key);
        if (v1 != null) return v1;

        ValueWrapper v2 = l2.get(key);
        if (v2 != null) {
            Object value = v2.get();
            if (value != null) {
                l1.put(key, value);
            }
            return v2;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(Object key, @Nullable Class<T> type) {
        Object v1 = l1.get(key, Object.class);
        if (v1 != null) return (T) v1;

        Object v2 = l2.get(key, Object.class);
        if (v2 != null) {
            l1.put(key, v2);
            return (T) v2;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper v1 = l1.get(key);
        if (v1 != null) {
            return (T) v1.get();
        }

        T value;
        try {
            value = l2.get(key, valueLoader);
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (value != null) {
            l1.put(key, value);
        }
        return value;
    }

    @Override
    public void put(Object key, @Nullable Object value) {
        if (value == null) return;
        l1.put(key, value);
        l2.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1.evict(key);
        l2.evict(key);
    }

    @Override
    public void clear() {
        l1.clear();
        l2.clear();
    }
}
