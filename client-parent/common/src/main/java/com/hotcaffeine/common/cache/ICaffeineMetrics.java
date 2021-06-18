package com.hotcaffeine.common.cache;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * caffeine统计
 * 
 * @author yongfeigao
 * @date 2021年6月2日
 */
public interface ICaffeineMetrics {
    /**
     * 统计
     * 
     * @param cache
     * @param name
     * @param maxSize
     * @return
     */
    public <T> ICaffeineMetrics metric(Cache<String, T> cache, String name, long maxSize);
}
