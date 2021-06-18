package com.hotcaffeine.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.hotcaffeine.common.util.ClientLogger;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;

/**
 * Caffeine指标
 * @author yongfeigao
 * @date 2021年1月28日
 */
public class DefaultCaffeineMetrics implements ICaffeineMetrics {

    public <T> ICaffeineMetrics metric(Cache<String, T> cache, String name, long maxSize) {
        // 如果之前有统计需要移除，否则会有问题
        try {
            removePrevMetric(name);
        } catch (Throwable e) {
            ClientLogger.getLogger().info("remove metric name:{} error:{}", name, e.toString());
        }
        CaffeineCacheMetrics.monitor(Metrics.globalRegistry, cache, name);
        return this;
    }

    /**
     * 移除之前的统计
     * 
     * @param name
     */
    private void removePrevMetric(String name) {
        Metrics.globalRegistry.forEachMeter(m -> {
            if (!isCacheName(m.getId().getName())) {
                return;
            }
            if (!name.equals(m.getId().getTag("cache"))) {
                return;
            }
            Metrics.globalRegistry.remove(m);
            ClientLogger.getLogger().info("remove metric:{}", m.getId());
        });
    }

    private boolean isCacheName(String name) {
        if ("cache.gets".equals(name)) {
            return true;
        }
        if ("cache.evictions".equals(name)) {
            return true;
        }
        if ("cache.size".equals(name)) {
            return true;
        }
        return false;
    }
}
