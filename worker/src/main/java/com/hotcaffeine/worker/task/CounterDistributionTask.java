package com.hotcaffeine.worker.task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.data.CounterDistributionStore;
import com.hotcaffeine.data.TopHotKeyStore;
import com.hotcaffeine.data.metric.CounterDistribution;
import com.hotcaffeine.data.metric.HotKey;
import com.hotcaffeine.data.metric.TopHotKey;
import com.hotcaffeine.worker.cache.AppCaffeineCache;
import com.hotcaffeine.worker.cache.AppKeyRuleCacher;
import com.hotcaffeine.worker.etcd.WorkerEtcdClient;
import com.hotcaffeine.worker.metric.BucketLeapArray;

import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 调用量分布任务
 * 
 * @author yongfeigao
 * @date 2021年2月8日
 */
@Component
public class CounterDistributionTask implements Destroyable {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private CounterDistributionStore counterDistributionStore;

    @Resource
    private TopHotKeyStore topHotKeyStore;

    @Resource
    private WorkerEtcdClient workerEtcdClient;
    
    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    private ThreadPoolExecutor counterDistributionExecutor;
    
    private ScheduledExecutorService scheduledExecutorService;
    
    @Autowired
    private AppCaffeineCache appCaffeineCache;

    @PostConstruct
    public void start() {
        counterDistributionExecutor = new ThreadPoolExecutor(10, 10,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(50),
                new ThreadFactoryBuilder().setNameFormat("counterDistributionExecutor-%d").setDaemon(true).build());
        scheduledExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder().setNameFormat("counterDistributionTask-%d").setDaemon(true).build());
        scheduledExecutorService.scheduleAtFixedRate(this::collect, 1000, 60000, TimeUnit.MILLISECONDS);
    }

    /**
     * 收集
     */
    public void collect() {
        try {
            int size = appCaffeineCache.getCaffeineCacheMap().size();
            if (size == 0) {
                return;
            }
            long time = System.currentTimeMillis();
            List<Future<?>> futureList = new ArrayList<>(size);
            // 遍历所有app
            appCaffeineCache.getCaffeineCacheMap().forEach((appName, cache) -> {
                futureList.add(counterDistributionExecutor.submit(() -> collect(appName, cache, time)));
            });
            futureList.forEach(f -> {
                try {
                    f.get();
                } catch (Throwable e) {
                    logger.error(e.getMessage(), e);
                }
            });
            // 设置时间
            workerEtcdClient.setTopkTaskTime(new SimpleDateFormat("yyyyMMddHHmm").format(new Date(time)));
            long use = System.currentTimeMillis() - time;
            if (use > 10000) {
                logger.info("collect use:{}", use);
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 收集
     * 
     * @param appName
     * @param cache
     */
    private void collect(String appName, CaffeineCache<BucketLeapArray> caffeineCache, long time) {
        // 执行清理
        caffeineCache.smartCleanup();
        Cache<String, BucketLeapArray> cache = caffeineCache.getCache();
        Map<String, CounterDistribution> counterDistributionMap = new HashMap<>();
        Map<String, TopHotKey> topHotKeyMap = new HashMap<>();
        ConcurrentMap<String, BucketLeapArray> cacheMap = cache.asMap();
        // 遍历整个缓存
        cacheMap.forEach((key, leapArray) -> {
            KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(appName);
            if(keyRuleCacher == null) {
                return;
            }
            KeyRule keyRule = keyRuleCacher.findRule(key);
            if (keyRule == null) {
                return;
            }
            // 统计topk
            if (keyRule.getTopkCount() > 0) {
                topHotKeyMap.computeIfAbsent(keyRule.getKey(), k -> new TopHotKey(keyRule.getTopkCount()))
                        .add(new HotKey(leapArray.getTotalCount(), leapArray.survivalTime(), keyRule.stripRuleKey(key)));
            }
            long count = (long) leapArray.leapArrayCount();
            if (count == 0) {
                // 没有调用重置活跃时间戳
                leapArray.resetActiveTimestamp();
                return;
            }
            // 统计调用量分布
            CounterDistribution counterDistribution = counterDistributionMap.computeIfAbsent(keyRule.getKey(),
                    k -> new CounterDistribution());
            boolean full = counterDistribution.incr(count, leapArray.liveTime());
            if (full) {
                logger.warn("{}{} full!", appName, key);
                return;
            }
        });
        // 存储调用分布
        storeCounterDistribution(appName, time, counterDistributionMap);
        // 存储topk
        storeTopk(appName, time, topHotKeyMap);
        long taskUse = System.currentTimeMillis() - time;
        logger.info("collect app:{} use:{} size:{}", appName, taskUse, cacheMap.size());
    }

    /**
     * 存储调用分布
     * 
     * @param appName
     * @param time
     * @param map
     */
    private void storeCounterDistribution(String appName, long time, Map<String, CounterDistribution> map) {
        map.forEach((ruleKey, counterDistribution) -> {
            long start = System.currentTimeMillis();
            try {
                counterDistributionStore.store(appName, ruleKey, time, counterDistribution);
            } catch (Throwable e) {
                logger.error("counterDistributionStore appName:{}, k:{}", appName, ruleKey, e);
            }
            long use = System.currentTimeMillis() - start;
            if (use > 100) {
                logger.info("store counterDistribution appName:{} ruleKey:{} use:{}", appName, ruleKey, use);
            }
        });
    }

    /**
     * 存储topk
     * 
     * @param appName
     * @param time
     * @param cache
     * @param map
     */
    private void storeTopk(String appName, long time, Map<String, TopHotKey> map) {
        map.forEach((ruleKey, topHotKey) -> {
            long start = System.currentTimeMillis();
            try {
                topHotKeyStore.storeWorkerData(appName, ruleKey, time, topHotKey);
            } catch (Throwable e) {
                logger.error("topHotKeyStore appName:{}, k:{}", appName, ruleKey, e);
            }
            long use = System.currentTimeMillis() - start;
            if (use > 100) {
                logger.info("store topHotKeyStore appName:{} ruleKey:{} use:{}", appName, ruleKey, use);
            }
        });
    }
    
    /**
     * 关闭
     */
    public void shutdown() {
        scheduledExecutorService.shutdown();
        counterDistributionExecutor.shutdown();
        while (!counterDistributionExecutor.isTerminated()) {
            logger.info("counterDistributionExecutor is terminating={} active={}",
                    counterDistributionExecutor.isTerminating(), counterDistributionExecutor.getActiveCount());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }

    @Override
    public int order() {
        return 2;
    }
}
