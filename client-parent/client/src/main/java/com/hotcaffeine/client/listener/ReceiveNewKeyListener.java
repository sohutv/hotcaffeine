package com.hotcaffeine.client.listener;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.hotcaffeine.client.HotCaffeine;
import com.hotcaffeine.client.HotCaffeineDetector;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.ValueModel;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.MetricsUtil;
import io.etcd.jetcd.shaded.com.google.common.eventbus.AllowConcurrentEvents;
import io.etcd.jetcd.shaded.com.google.common.eventbus.Subscribe;
import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 客户端监听到有newKey事件
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
public class ReceiveNewKeyListener {

    private HotCaffeineDetector hotCaffeineDetector;

    public ReceiveNewKeyListener(HotCaffeineDetector hotCaffeineDetector) {
        this.hotCaffeineDetector = hotCaffeineDetector;
        if (hotCaffeineDetector.getKeyListenerExecutor() == null) {
            // 初始化热点key线程池
            ExecutorService newKeyExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(100),
                    new ThreadFactoryBuilder().setNameFormat(
                            "hotcaffeine-listener-" + hotCaffeineDetector.getAppName() + "-%d").setDaemon(true).build());
            hotCaffeineDetector.setKeyListenerExecutor(newKeyExecutor);
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void newKeyComing(ReceiveNewKeyEvent event) {
        KeyCount keyCount = event.getKeyCount();
        if (keyCount == null) {
            return;
        }
        // 收到新key推送
        newKey(keyCount);
    }

    public void newKey(KeyCount keyCount) {
        long now = System.currentTimeMillis();
        String key = keyCount.getKey();
        if (key == null || key.length() == 0) {
            ClientLogger.getLogger().warn("the keyCount:{} key is blank", keyCount);
            return;
        }
        MetricsUtil.incrReceiveKeys(1);
        // 如果key到达时已经过去1秒了
        if (keyCount.getCreateTime() != 0 && Math.abs(now - keyCount.getCreateTime()) > 1000) {
            ClientLogger.getLogger().warn("the key:{} time difference:{}, now:{} keyCreateAt:{}", key,
                    (now - keyCount.getCreateTime()), now, keyCount.getCreateTime());
        }
        // 获取缓存
        KeyRule keyRule = hotCaffeineDetector.getKeyRuleCacher().findRule(key);
        if (keyRule == null) {
            ClientLogger.getLogger().warn("keyCount:{} keyRule is null", keyCount);
            return;
        }
        CacheRule cacheRule = hotCaffeineDetector.getKeyRuleCacher().getCacheRule(keyRule);
        if(cacheRule == null) {
            ClientLogger.getLogger().warn("keyCount:{} cacheRule is null", keyCount);
            return;
        }
        String realKey = keyRule.stripRuleKey(key);
        // 删除事件
        if (keyCount.isRemove()) {
            ClientLogger.getLogger().warn("key:{} deleted!", key);
            cacheRule.getCache().delete(realKey);
            return;
        }
        
        // 判断重复热点推送
        ValueModel existValue = cacheRule.getCache().get(realKey);
        if (existValue != null) {
            long leftTime = existValue.leftTime(now);
            if (!existValue.isLeftTimeNearExpire(leftTime)) {
                // 重复热点只记录日志，需要重新设置值，否则各个客户端缓存时间不一致，可能导致各个客户端不断的上报
                ClientLogger.getLogger().warn("repeat hot key:{} leftTime:{}", key, leftTime);
                // topk推送过来的不强制更新
                if (keyCount.getCreateTime() == 0) {
                    return;
                }
            }
        }
        
        // 构造热点值
        ValueModel valueModel = new ValueModel();
        valueModel.setDuration(cacheRule.getDuration() * 1000);
        // 获取hotCaffeine
        HotCaffeine hotCaffeine = hotCaffeineDetector.getHotCaffeine(keyRule);
        if (hotCaffeine == null) {
            cacheRule.getCache().set(realKey, valueModel);
            return;
        }
        IKeyListener keyListener = hotCaffeine.getKeyListener();
        // 没有对应的值获取器，直接设置热点缓存
        if (keyListener == null) {
            cacheRule.getCache().set(realKey, valueModel);
            return;
        }
        // 不需要缓存值，先设为热key，再异步通知
        if (!keyListener.needCacheValue()) {
            cacheRule.getCache().set(realKey, valueModel);
        }
        // 异步执行，防止阻塞netty
        hotCaffeineDetector.getKeyListenerExecutor().execute(() -> {
            Object value = null;
            try {
                value = keyListener.hot(keyCount.getKey());
            } catch (Exception e) {
                ClientLogger.getLogger().warn("fetch key:{} error:{}", key, e.toString());
            }
            // 需要缓存value，得等客户端的值，不能提前设置热key
            if (keyListener.needCacheValue()) {
                valueModel.setValue(value);
                if (ClientLogger.getLogger().isDebugEnabled()) {
                    ClientLogger.getLogger().debug("key:{} set value async!", key);
                }
                cacheRule.getCache().set(realKey, valueModel);
            }
        });
    }
    
    public static class ReceiveNewKeyEvent {
        private KeyCount keyCount;

        public ReceiveNewKeyEvent(KeyCount keyCount) {
            this.keyCount = keyCount;
        }

        public KeyCount getKeyCount() {
            return keyCount;
        }

        public void setKeyCount(KeyCount keyCount) {
            this.keyCount = keyCount;
        }
    }
}
