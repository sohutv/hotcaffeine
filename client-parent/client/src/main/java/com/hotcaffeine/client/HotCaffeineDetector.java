package com.hotcaffeine.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hotcaffeine.client.count.DefaultKeyCounter;
import com.hotcaffeine.client.etcd.AppEtcdClient;
import com.hotcaffeine.client.listener.IKeyListener;
import com.hotcaffeine.client.listener.ReceiveNewKeyListener;
import com.hotcaffeine.client.listener.ReceiveNewKeyListener.ReceiveNewKeyEvent;
import com.hotcaffeine.client.listener.RequestHotValueListener;
import com.hotcaffeine.client.listener.UnsupportedMessageTypeListener;
import com.hotcaffeine.client.listener.WorkerChangeListener;
import com.hotcaffeine.client.netty.NettyClient;
import com.hotcaffeine.client.push.HashGroupPusher;
import com.hotcaffeine.client.worker.HealthDetector;
import com.hotcaffeine.common.etcd.DefaultEtcdConfig;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.EventBusUtil;
import com.hotcaffeine.common.util.ServiceLoaderUtil;

/**
 * 热键探测器
 * 
 * @author yongfeigao
 * @date 2021年1月15日
 */
public class HotCaffeineDetector {
    // 默认key的push间隔，毫秒
    public static final int DEFAULT_PUSH_INTERVAL = 500;
    
    public static final String PREFIX = "prefix";

    // key计数
    private DefaultKeyCounter keyCounter;

    // key pusher
    private HashGroupPusher keyPusher;

    // netty客户端
    private NettyClient nettyClient;

    // 推送key的间隔(毫秒)
    private long pushInterval = DEFAULT_PUSH_INTERVAL;
    
    // key最长多少
    private int maxKeyLength = 1024;

    // 规则缓存器
    private KeyRuleCacher keyRuleCacher;

    // 应用名称，在dashboard申请
    private String appName;

    // 热点缓存map
    private Map<String, HotCaffeine> hotCaffeineCacheMap = new HashMap<>();

    // 数据抓取线程池
    private ExecutorService keyListenerExecutor;

    // 配置中心
    private AppEtcdClient appEtcdClient;

    // 自己
    private static HotCaffeineDetector instance;
    
    // 本地热键检测缓存
    private Cache<String, AtomicInteger> hotDetectCache;
    
    // worker健康探测
    private HealthDetector workerHealthDetector;
    
    /**
     * 一个app一个实例
     * 
     * @param appName
     */
    public HotCaffeineDetector(IEtcdConfig etcdConfig) {
        if (instance != null) {
            throw new IllegalArgumentException("Only one instance in jvm!");
        }
        this.appName = etcdConfig.getUser();
        // worker健康探测
        this.workerHealthDetector = new HealthDetector();
        // 创建netty客户端
        this.nettyClient = new NettyClient(appName);
        // 创建规则缓存器
        this.keyRuleCacher = new KeyRuleCacher(appName);
        // 创建key计数器
        this.keyCounter = new DefaultKeyCounter();
        // 创建配置中心
        this.appEtcdClient = new AppEtcdClient(etcdConfig);
        // 静态引用，供aop使用
        instance = this;
    }

    /**
     * 启动任务
     */
    public void start() {
        try {
            // netty启动
            nettyClient.start();
            // 创建key推送器并启动
            this.keyPusher = buildKeyPusher();
            this.keyPusher.start();
            // 注册事件
            registEventBus();
            // init etcd client and start
            this.appEtcdClient.start();
            // initLocalDetectCache
            initLocalDetectCache();
            // workerHealthDetector
            workerHealthDetector.setHotCaffeineDetector(this);
            workerHealthDetector.start();
            ClientLogger.getLogger().info("HotCaffeineDetector:{} started", appName);
        } catch (Throwable e) {
            ClientLogger.getLogger().error("appName:{} start error", appName, e);
        }
    }
    
    public void initLocalDetectCache() {
        hotDetectCache = Caffeine.newBuilder()
                .initialCapacity(128)// 初始大小
                .maximumSize(256)// 最大数量
                .expireAfterWrite(3, TimeUnit.SECONDS)// 3秒内只通知一次
                .build();
    }

    /**
     * 关闭
     */
    public void shutdown() {
        try {
            this.keyCounter.shutdown();
            this.keyPusher.shutdown();
            this.appEtcdClient.shutdown();
            this.nettyClient.shutdown();
            ClientLogger.getLogger().info("HotCaffeineDetector shutdown");
        } catch (Exception e) {
            ClientLogger.getLogger().warn("shutdown error, maybe ignore if at shutting down:{}", e.toString());
        }
    }

    /**
     * 构建key推送器
     * 
     * @return HashGroupPusher
     */
    private HashGroupPusher buildKeyPusher() {
        // 构建pusher
        HashGroupPusher keyPusher = new HashGroupPusher();
        keyPusher.setAppName(appName);
        keyPusher.setNettyClient(nettyClient);
        keyPusher.setKeyCollector(keyCounter);
        keyPusher.setPushInterval(pushInterval);
        keyPusher.setType(MessageType.REQUEST_NEW_KEY);
        return keyPusher;
    }

    /**
     * 注册事件
     */
    private void registEventBus() {
        // netty连接器会关注WorkerInfoChangeEvent事件
        EventBusUtil.register(new WorkerChangeListener(nettyClient));
        // 热key探测回调关注热key事件
        EventBusUtil.register(new ReceiveNewKeyListener(this));
        // Rule的变化的事件
        EventBusUtil.register(keyRuleCacher);
        // 热key值回调事件
        EventBusUtil.register(new RequestHotValueListener(keyRuleCacher));
        // 不支持消息回调事件
        EventBusUtil.register(new UnsupportedMessageTypeListener());
    }

    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @return HotCaffeine
     */
    public HotCaffeine build() {
        return build(KeyRule.DEFAULT_KEY);
    }
    
    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @return HotCaffeine
     */
    public HotCaffeine buildPrefix() {
        return buildPrefix(null);
    }
    
    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @return HotCaffeine
     */
    public HotCaffeine buildPrefix(IKeyListener listener) {
        return hotCaffeineCacheMap.computeIfAbsent(PREFIX, k -> new PrefixHotCaffeine(k, listener, HotCaffeineDetector.this));
    }

    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @return HotCaffeine
     */
    public HotCaffeine build(String ruleKey) {
        return build(ruleKey, null);
    }

    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @return HotCaffeine
     */
    public HotCaffeine build(IKeyListener listener) {
        return build(KeyRule.DEFAULT_KEY, listener);
    }

    /**
     * 构建热点缓存(此方法非线程安全)
     * 
     * @param ruleKey
     * @return HotCaffeine
     */
    public HotCaffeine build(String ruleKey, IKeyListener listener) {
        return hotCaffeineCacheMap.computeIfAbsent(ruleKey, k -> new HotCaffeine(k, listener, HotCaffeineDetector.this));
    }

    /**
     * 获取默认热点缓存
     * 
     * @return HotCaffeine
     */
    public HotCaffeine getHotCaffeine() {
        return getHotCaffeine(KeyRule.DEFAULT_KEY);
    }

    /**
     * 获取热点缓存
     * 
     * @param keySource
     * @return HotCaffeine
     */
    public HotCaffeine getHotCaffeine(String ruleKey) {
        return hotCaffeineCacheMap.get(ruleKey);
    }
    
    /**
     * 获取热咖啡
     * 
     * @param keySource
     * @return HotCaffeine
     */
    public HotCaffeine getHotCaffeine(KeyRule keyRule) {
        if (keyRule.isPrefix()) {
            return getHotCaffeine(PREFIX);
        }
        return hotCaffeineCacheMap.get(keyRule.getKey());
    }
    
    /**
     * 本地热key检测，如果达到阈值，发布热key事件
     * 
     * @param fullKey
     * @param count
     */
    public void localDetect(String fullKey, long count, int threshold, double thresholdQps) {
        double qps = count / (pushInterval / 1000d);
        if (count <= threshold || qps <= thresholdQps) {
            return;
        }
        // 并发安全，保障hotCounter只有一个
        AtomicInteger counter = hotDetectCache.get(fullKey, k -> new AtomicInteger());
        // 已经执行过直接返回，保障只通知一次
        if (counter.incrementAndGet() > 1) {
            return;
        }
        KeyCount keyCount = new KeyCount();
        keyCount.setKey(fullKey);
        EventBusUtil.post(new ReceiveNewKeyEvent(keyCount));
        ClientLogger.getLogger().info("fullKey:{} hot local, count:{} threshold:{} qps:{} thresholdQps:{}", 
                fullKey, count, threshold, qps, thresholdQps);
    }
    
    /**
     * 构建器
     * 
     * @author yongfeigao
     * @date 2021年1月26日
     */
    public static class Builder {
        // appName
        private String appName;
        
        private String endpoints;
        
        private String rootPath;

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder endpoints(String endpoints) {
            this.endpoints = endpoints;
            return this;
        }

        public Builder rootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public HotCaffeineDetector build() {
            IEtcdConfig etcdConfig = ServiceLoaderUtil.loadService(IEtcdConfig.class, DefaultEtcdConfig.class);
            if (endpoints != null) {
                etcdConfig.setEndpoints(endpoints);
            }
            if (rootPath != null) {
                etcdConfig.setRootPath(rootPath);
            }
            etcdConfig.init(appName);
            return new HotCaffeineDetector(etcdConfig);
        }
    }

    public long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(long pushInterval) {
        if(pushInterval < DEFAULT_PUSH_INTERVAL) {
            pushInterval = DEFAULT_PUSH_INTERVAL;
        }
        this.pushInterval = pushInterval;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public DefaultKeyCounter getKeyCounter() {
        return keyCounter;
    }

    public KeyRuleCacher getKeyRuleCacher() {
        return keyRuleCacher;
    }

    public ExecutorService getKeyListenerExecutor() {
        return keyListenerExecutor;
    }

    public void setKeyListenerExecutor(ExecutorService keyListenerExecutor) {
        this.keyListenerExecutor = keyListenerExecutor;
    }

    public int getMaxKeyLength() {
        return maxKeyLength;
    }

    public void setMaxKeyLength(int maxKeyLength) {
        this.maxKeyLength = maxKeyLength;
    }

    public static HotCaffeineDetector getInstance() {
        return instance;
    }

    public NettyClient getNettyClient() {
        return nettyClient;
    }

    public HealthDetector getWorkerHealthDetector() {
        return workerHealthDetector;
    }
}
