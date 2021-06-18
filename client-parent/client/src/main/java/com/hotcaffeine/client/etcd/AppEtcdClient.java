package com.hotcaffeine.client.etcd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.hotcaffeine.client.listener.ReceiveNewKeyListener.ReceiveNewKeyEvent;
import com.hotcaffeine.client.listener.WorkerChangeListener.WorkerChangeEvent;
import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.EventType;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher.CacheRuleChangeEvent;
import com.hotcaffeine.common.model.KeyRuleCacher.KeyRuleChangeEvent;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.EventBusUtil;
import com.hotcaffeine.common.util.JsonUtil;

import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.internal.StringUtil;

/**
 * app etcd客户端
 * 
 * @author yongfeigao
 * @date 2021年1月25日
 */
public class AppEtcdClient {

    // 空白list，禁止修改
    protected List<KV> BLANK_LIST = Collections.unmodifiableList(new ArrayList<>());

    // 获取server地址间隔
    private int fetchWorkerInterval = 30;

    private EtcdClient etcdClient;
    
    private IEtcdConfig etcdConfig;

    // 任务执行
    private ScheduledExecutorService workerInfoExecutorService;

    public AppEtcdClient(IEtcdConfig etcdConfig) {
        this.etcdClient = new EtcdClient(etcdConfig.getEndpoints(), etcdConfig.getUser(), etcdConfig.getPassword());
        this.etcdConfig = etcdConfig;
        workerInfoExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                "hotcaffeine-fetch-worker-" + etcdConfig.getUser() + "-%d").setDaemon(true).build());
    }

    /**
     * 启动
     */
    public AppEtcdClient start() {
        // 获取worker地址
        startFetchWorkList();
        // 抓取keyCache
        fetchCacheRule();
        // 监控keyCache
        startWatchKeyCache();
        // 抓取规则
        fetchRule();
        // 监控规则
        startWatchRule();
        // 监听热key事件，只监听手工添加、删除的key
        startWatchHotKey();
        return this;
    }

    /**
     * 启动后先拉取已存在的热key
     */
    private void fetchExistHotKey() {
        List<KV> kvList = getHotKeyList();
        kvList.forEach(kv -> {
            KeyCount keyCount = new KeyCount();
            keyCount.setKey(kv.getKey());
            EventBusUtil.post(new ReceiveNewKeyEvent(keyCount));
        });
    }

    /**
     * 每隔fetchWorkerInterval秒拉取worker信息
     */
    private void startFetchWorkList() {
        workerInfoExecutorService.scheduleAtFixedRate(() -> {
            try {
                fetchWorkList();
            } catch (Exception e) {
                ClientLogger.getLogger().warn("fetchWorkList err:{}", e.toString());
            }
        }, 0, fetchWorkerInterval, TimeUnit.SECONDS);
        ClientLogger.getLogger().info("etcd client start fetch worker task, intervel {}s", fetchWorkerInterval);
    }

    // 拉取work ip
    private void fetchWorkList() {
        List<KV> kvList = getWorkerList();
        Set<String> addresses = new HashSet<>();
        kvList.forEach(kv -> {
            addresses.add(kv.getValue());
        });
        // 发布workerinfo变更信息
        EventBusUtil.post(new WorkerChangeEvent(addresses));
    }

    /**
     * 异步开始监听热key变化信息，该目录里只有手工添加的key信息
     */
    private void startWatchHotKey() {
        String key = etcdConfig.getUserHotKeyPath();
        watchHotKey(kv -> {
            KeyCount keyCount = new KeyCount();
            stripPrefix(key, kv);
            keyCount.setKey(kv.getKey());
            if (EventType.DELETE == kv.getEventType()) {
                keyCount.setRemove(true);
            } else {
                long time = -1;
                try {
                    time = Long.valueOf(kv.getValue());
                } catch (NumberFormatException e) {
                }
                if (time == -1) {
                    return;
                }
                // 手工创建的value是时间戳
                keyCount.setCreateTime(time);
            }
            EventBusUtil.post(new ReceiveNewKeyEvent(keyCount));
        });
    }

    private void fetchRule() {
        while (true) {
            boolean success = fetchKeyRule();
            if (success) {
                // 拉取已存在的热key
                fetchExistHotKey();
                break;
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                ClientLogger.getLogger().warn("fetchRule error:{}", e.toString());
            }
        }
    }

    private boolean fetchKeyRule() {
        String rulePath = etcdConfig.getUserRulePath();
        String rule = etcdClient.get(rulePath);
        List<KeyRule> ruleList = new ArrayList<>();
        if (StringUtil.isNullOrEmpty(rule)) {
            ClientLogger.getLogger().warn("fetchKeyRule:{} value is empty", rulePath);
            // 会清空本地缓存队列
            EventBusUtil.post(new KeyRuleChangeEvent(ruleList));
            return true;
        }
        ruleList = JsonUtil.toList(rule, KeyRule.class);
        EventBusUtil.post(new KeyRuleChangeEvent(ruleList));
        return true;
    }

    private boolean fetchCacheRule() {
        String keyCachePath = etcdConfig.getUserCachePath();
        String cacheRuleString = etcdClient.get(keyCachePath);
        if (StringUtil.isNullOrEmpty(cacheRuleString)) {
            ClientLogger.getLogger().warn("fetchCacheRule:{} value is empty", keyCachePath);
            // 会清空本地缓存队列
            EventBusUtil.post(new CacheRuleChangeEvent(new ArrayList<>(0)));
            return true;
        }
        List<CacheRule> cacheRuleList = JsonUtil.toList(cacheRuleString, CacheRule.class);
        EventBusUtil.post(new CacheRuleChangeEvent(cacheRuleList));
        return true;
    }

    /**
     * 异步监听rule规则变化
     */
    private void startWatchRule() {
        String watchPath = etcdConfig.getUserRulePath();
        etcdClient.watch(watchPath, kv -> {
            fetchKeyRule();
        });
    }

    /**
     * 异步监听cache规则变化
     */
    private void startWatchKeyCache() {
        String keyCachePath = etcdConfig.getUserCachePath();
        etcdClient.watch(keyCachePath, kv -> {
            fetchCacheRule();
        });
    }

    public void setFetchWorkerInterval(int fetchWorkerInterval) {
        this.fetchWorkerInterval = fetchWorkerInterval;
    }

    public void shutdown() {
        workerInfoExecutorService.shutdown();
        etcdClient.close();
    }

    /**
     * 获取热键
     * 
     * @return
     */
    public List<KV> getHotKeyList() {
        String key = etcdConfig.getUserHotKeyPath();
        List<KV> list = etcdClient.getPrefix(key);
        return stripPrefix(key, list);
    }

    /**
     * 获取worker
     * 
     * @return
     */
    public List<KV> getWorkerList() {
        String key = etcdConfig.getUserWorkerPath();
        List<KV> kvList = etcdClient.getPrefix(key);
        if (kvList == null || kvList.isEmpty()) {
            key = etcdConfig.getDefaultWorkerPath();
            kvList = etcdClient.getPrefix(key);
        }
        if (kvList == null || kvList.isEmpty()) {
            ClientLogger.getLogger().warn("value is blank:{}", key);
            return BLANK_LIST;
        }
        return kvList;
    }

    /**
     * 监控热键
     * 
     * @param consumer
     */
    public void watchHotKey(Consumer<KV> consumer) {
        etcdClient.watch(etcdConfig.getUserHotKeyPath(), consumer);
    }

    /**
     * 剥离前缀
     * 
     * @param key
     * @param list
     * @return
     */
    protected List<KV> stripPrefix(String key, List<KV> list) {
        if (list == null || list.size() == 0) {
            ClientLogger.getLogger().info("value is blank:{}", key);
            return BLANK_LIST;
        }
        list.forEach(kv -> {
            stripPrefix(key, kv);
        });
        return list;
    }

    protected void stripPrefix(String key, KV kv) {
        String newKey = kv.getKey().replace(key + "/", "");
        kv.setKey(newKey);
    }

    public void close() {
        etcdClient.close();
    }
}
