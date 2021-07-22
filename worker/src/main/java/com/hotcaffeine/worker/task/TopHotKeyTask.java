package com.hotcaffeine.worker.task;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.data.TopHotKeyStore;
import com.hotcaffeine.data.metric.HotKey;
import com.hotcaffeine.data.metric.TopHotKey;
import com.hotcaffeine.worker.cache.AppKeyRuleCacher;
import com.hotcaffeine.worker.etcd.WorkerEtcdClient;
import com.hotcaffeine.worker.pusher.IPusher;

import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * tophotkey任务
 * 
 * @author yongfeigao
 * @date 2021年3月9日
 */
@Component
public class TopHotKeyTask implements Destroyable {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Logger hotKeyLogger = LoggerFactory.getLogger("hotkey");

    private ThreadPoolExecutor topHotKeyExecutor;

    private ScheduledExecutorService scheduledExecutorService;

    @Resource
    private TopHotKeyStore topHotKeyStore;

    @Resource
    private WorkerEtcdClient etcdStarter;

    @Resource
    private List<IPusher> iPushers;

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    public TopHotKeyTask() {
        topHotKeyExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1,
                Runtime.getRuntime().availableProcessors() + 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(3000),
                new ThreadFactoryBuilder().setNameFormat("topHotKeyExecutor-%d").setDaemon(true).build());
        scheduledExecutorService = Executors.newScheduledThreadPool(2,
                new ThreadFactoryBuilder().setNameFormat("topHotKeyTask-%d").setDaemon(true).build());
        scheduledExecutorService.scheduleAtFixedRate(this::mergeAndPush, 1000, 3000, TimeUnit.MILLISECONDS);
    }

    /**
     * 合并并推送
     */
    public void mergeAndPush() {
        try {
            long now = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            // 获取merge时间
            String time = topHotKeyStore.getMergeTime();
            // 没有进行初始化
            if (time == null) {
                time = sdf.format(new Date(now));
                topHotKeyStore.setMergeTime(time);
            }
            // 检测是否可以merge
            boolean ok = etcdStarter.topkMergeTaskTimeOK(time);
            if (!ok) {
                return;
            }
            // 设置下次merge时间
            Date nextTime = sdf.parse(time);
            nextTime.setTime(nextTime.getTime() + 60000);
            topHotKeyStore.setMergeTime(sdf.format(nextTime));

            final String timeKey = time;
            appKeyRuleCacher.getAppKeyRuleCacherMap().forEach((appName, keyRuleCacher) -> {
                keyRuleCacher.getKeyRuleMap().forEach((k, keyRule) -> {
                    mergeAndPush(appName, keyRule, timeKey);
                });
                keyRuleCacher.getPrefixKeyRuleList().forEach(keyRule -> {
                    mergeAndPush(appName, keyRule, timeKey);
                });
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 合并推送
     * 
     * @param appName
     * @param keyRule
     * @param timeKey
     */
    private void mergeAndPush(String appName, KeyRule keyRule, String timeKey) {
        topHotKeyExecutor.execute(() -> {
            try {
                TopHotKey topHotKey = topHotKeyStore.merge(appName, keyRule.getKey(), timeKey, keyRule.getTopkCount());
                if (topHotKey == null) {
                    return;
                }
                logger.info("merge time:{} app:{} rule:{}", timeKey, appName, keyRule.getKey());
                if (topHotKey.getHotKeyList() == null) {
                    return;
                }
                if (!keyRule.isUseTopKAsHotKey()) {
                    return;
                }
                push(appName, topHotKey.getHotKeyList());
            } catch (Exception e) {
                hotKeyLogger.error(e.getMessage(), e);
            }
        });
    }

    /**
     * 推送topkey
     * 
     * @param cache
     * @param appName
     * @param k
     * @param v
     */
    private void push(String appName, List<HotKey> hotKeyList) {
        KeyCount keyCount = new KeyCount();
        keyCount.setAppName(appName);
        for (HotKey hotKey : hotKeyList) {
            keyCount.setKey(hotKey.getKey());
            hotKeyLogger.info("appName:{} key:{} topk hot", appName, hotKey.getKey());
            // 分别推送到各client和dashboard
            for (IPusher pusher : iPushers) {
                pusher.push(keyCount);
            }
        }
    }

    /**
     * 关闭
     */
    public void shutdown() {
        scheduledExecutorService.shutdown();
        topHotKeyExecutor.shutdown();
        while (!topHotKeyExecutor.isTerminated()) {
            logger.info("counterDistributionExecutor is terminating={} active={}",
                    topHotKeyExecutor.isTerminating(), topHotKeyExecutor.getActiveCount());
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
        return 1;
    }
}
