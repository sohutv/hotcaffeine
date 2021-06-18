package com.hotcaffeine.common.cache;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.ServiceLoaderUtil;

/**
 * caffine缓存对localCache的实现
 * 
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public class CaffeineCache<T> implements LocalCache<T> {
    // 活跃时间
    private long activeTime;
    
    private long duration;
    
    private String name;
    
    private Cache<String, T> cache;

    public CaffeineCache(String name, int cacheSize, int duration) {
        this(name, Caffeine.newBuilder()
                .initialCapacity(128)// 初始大小
                .maximumSize(cacheSize)// 最大数量
                .expireAfterWrite(duration, TimeUnit.SECONDS)// 过期时间
                .recordStats(), TimeUnit.SECONDS.toMillis(duration), cacheSize);
        ClientLogger.getLogger().info("init cache:{} size:{} duration:{}", name, cacheSize, duration);
    }

    public CaffeineCache(String name, Caffeine<Object, Object> caffeine, long expireInMillis, long maximumSize) {
        this.name = name;
        this.duration = expireInMillis;
        cache = caffeine.build();
        ICaffeineMetrics caffeineMetrics = ServiceLoaderUtil.loadService(ICaffeineMetrics.class, DefaultCaffeineMetrics.class);
        caffeineMetrics.metric(cache, name, maximumSize);
    }

    @Override
    public T get(String key) {
        return get(key, null);
    }

    @Override
    public T get(String key, T defaultValue) {
        T o = cache.getIfPresent(key);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }
    
    @Override
    public void delete(String key) {
        cache.invalidate(key);
    }

    @Override
    public void set(String key, T value) {
        cache.put(key, value);
    }

    @Override
    public void removeAll() {
        cache.invalidateAll();
    }

    @Override
    public Cache<String, T> getCache() {
        return cache;
    }

    public void setActiveTime(long activeTime) {
        this.activeTime = activeTime;
    }

    public void smartCleanup() {
        if (cache.estimatedSize() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long freeTime = now - activeTime;
        if (freeTime > 2 * duration) {
            cache.cleanUp();
            ClientLogger.getLogger().info("free time:{} cleanup:{} use:{}", freeTime, name,
                    (System.currentTimeMillis() - now));
        }
    }
}
