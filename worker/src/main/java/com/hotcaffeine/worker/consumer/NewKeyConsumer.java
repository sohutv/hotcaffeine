package com.hotcaffeine.worker.consumer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.common.util.MemoryMQ.MemoryMQConsumer;
import com.hotcaffeine.common.util.MetricsUtil;
import com.hotcaffeine.worker.cache.AppCaffeineCache;
import com.hotcaffeine.worker.cache.AppKeyRuleCacher;
import com.hotcaffeine.worker.metric.BucketLeapArray;
import com.hotcaffeine.worker.pusher.IPusher;

/**
 * 新key消费
 * 
 * @author yongfeigao
 * @date 2021年1月12日
 */
@Component
public class NewKeyConsumer implements MemoryMQConsumer<KeyCount> {
    private Logger logger = LoggerFactory.getLogger("hotcaffeine");

    private Cache<String, AtomicInteger> recentCache;

    @Resource
    private List<IPusher> iPushers;

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @Autowired
    private AppCaffeineCache appCaffeineCache;

    public NewKeyConsumer() {
        initRecentCache();
    }

    public void initRecentCache() {
        long maximumSize = 50000;
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(256)// 初始大小
                .maximumSize(maximumSize)// 最大数量
                .expireAfterWrite(5, TimeUnit.SECONDS)// 过期时间
                .softValues()
                .recordStats();
        CaffeineCache<AtomicInteger> caffeineCache = new CaffeineCache<>("recentCache", caffeine,
                TimeUnit.SECONDS.toMillis(5), maximumSize);
        recentCache = caffeineCache.getCache();
    }

    public void consume(KeyCount keyCount) {
        // 统计
        MetricsUtil.incrDealKeys();
        // 唯一key
        String uniqueKey = keyCount.getKey();
        // 判断是不是刚热不久
        AtomicInteger hotCounter = recentCache.getIfPresent(uniqueKey);
        if (hotCounter != null) {
            return;
        }
        // 获取key规则
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(keyCount.getAppName());
        if (keyRuleCacher == null) {
            return;
        }
        String key = keyCount.getKey();
        KeyRule keyRule = keyRuleCacher.findRule(key);
        if (keyRule == null) {
            return;
        }
        // 获取缓存
        CaffeineCache<BucketLeapArray> caffeineCache = appCaffeineCache.getCacheOrBuildIfAbsent(keyCount.getAppName());
        caffeineCache.setActiveTime(System.currentTimeMillis());
        // 获取滑动窗口
        BucketLeapArray leapArray = getBucketLeapArray(caffeineCache, key, keyRule);
        long count = leapArray.count(keyCount.getCount());
        // 没hot
        if (count < keyRule.getThreshold()) {
            return;
        }
        // 并发安全，保障hotCounter只有一个
        hotCounter = recentCache.get(uniqueKey, k -> new AtomicInteger());
        // 已经执行过直接返回，保障只通知一次
        if (hotCounter.incrementAndGet() > 1) {
            return;
        }
        // 删掉该key
        caffeineCache.delete(key);
        // 开启推送
        keyCount.setCreateTime(System.currentTimeMillis());
        logger.info("appName:{} key:{} inner:{} rule:{} hot:{}", keyCount.getAppName(), key, keyCount.isInner(), keyRule.getKey(), count);
        // 分别推送到各client和dashboard
        MetricsUtil.incrSendKeys();
        for (IPusher pusher : iPushers) {
            pusher.push(keyCount);
        }
    }

    /**
     * 获取滑动窗口
     * 
     * @param appName
     * @param key
     * @param keyRule
     * @return
     */
    private BucketLeapArray getBucketLeapArray(CaffeineCache<BucketLeapArray> caffeineCache, String key,
            KeyRule keyRule) {
        BucketLeapArray bucketLeapArray = caffeineCache.getCache().getIfPresent(key);
        if(bucketLeapArray == null) {
            bucketLeapArray = new BucketLeapArray(getSampleCount(keyRule.getInterval()), keyRule.getInterval() * 1000);
            caffeineCache.getCache().put(key, bucketLeapArray);
        }
        return bucketLeapArray;
    }

    private int getSampleCount(int interval) {
        // 2秒及以下，每秒2窗口
        if (interval <= 2) {
            return interval * 2;
        }
        // 3秒及以上每秒一个窗口
        return interval;
    }

    public Cache<String, AtomicInteger> getRecentCache() {
        return recentCache;
    }
}
