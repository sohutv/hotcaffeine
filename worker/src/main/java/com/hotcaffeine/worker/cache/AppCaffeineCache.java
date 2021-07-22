package com.hotcaffeine.worker.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.worker.metric.BucketLeapArray;

/**
 * app缓存
 * 
 * @author yongfeigao
 * @date 2021年4月7日
 */
@Component
public class AppCaffeineCache {
    public static final long MAXIMUM_SIZE = 5000000;
    
    // 缓存过期时间
    @Value("${caffeine.expireInSeconds}")
    private int expireInSeconds;

    /**
     * key是appName，value是caffeine
     */
    private ConcurrentMap<String, CaffeineCache<BucketLeapArray>> caffeineCacheMap = new ConcurrentHashMap<>();

    /**
     * 获取cache，并发安全
     * 
     * @param appName
     * @return
     */
    public CaffeineCache<BucketLeapArray> getCacheOrBuildIfAbsent(String appName) {
        return caffeineCacheMap.computeIfAbsent(appName, key -> {
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .initialCapacity(8192)
                    .maximumSize(MAXIMUM_SIZE)
                    .expireAfterAccess(expireInSeconds, TimeUnit.SECONDS)
                    .softValues()
                    .recordStats();
            return new CaffeineCache<>(appName, caffeine, TimeUnit.SECONDS.toMillis(expireInSeconds), MAXIMUM_SIZE);
        });
    }
    
    /**
     * 获取cache，并发安全
     * 
     * @param appName
     * @return
     */
    public CaffeineCache<BucketLeapArray> getCache(String appName) {
        return caffeineCacheMap.get(appName);
    }
    

    public ConcurrentMap<String, CaffeineCache<BucketLeapArray>> getCaffeineCacheMap() {
        return caffeineCacheMap;
    }

    /**
     * 清空某个app的缓存key
     */
    public void clearCacheByAppName(String appName) {
        if (caffeineCacheMap.get(appName) != null) {
            caffeineCacheMap.get(appName).removeAll();
        }
    }
}
