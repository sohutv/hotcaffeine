package com.hotcaffeine.client.worker;

import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.hotcaffeine.client.HotCaffeineDetector;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.JsonUtil;

import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * worker健康探测
 * 
 * @author yongfeigao
 * @date 2021年7月16日
 */
public class HealthDetector implements HealthDetectorMBean {
    // 最大响应时间
    public static final int MAX_RESPONSE_TIME_IN_MILLIS = 500;

    // worker状态map
    private ConcurrentMap<String, WorkerStat> workerStatMap = new ConcurrentHashMap<>();

    // 健康
    private volatile boolean healthy = true;

    private HotCaffeineDetector hotCaffeineDetector;

    // 任务执行
    private ScheduledExecutorService sendKeyExecutorService;

    // 任务执行
    private ScheduledExecutorService detectExecutorService;

    /**
     * 启动任务
     */
    public void start() {
        sendKeyExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                "sendKeyExecutorService").setDaemon(true).build());
        // 定时发送检测key
        sendKeyExecutorService.scheduleAtFixedRate(() -> {
            try {
                sendDetectKey();
            } catch (Throwable e) {
                ClientLogger.getLogger().error("workerHealthDetector sendDetectKey error:{}", e.getMessage());
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);
        // 定时检测任务
        detectExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                "detectExecutorService").setDaemon(true).build());
        detectExecutorService.scheduleAtFixedRate(() -> {
            try {
                detect();
            } catch (Throwable e) {
                ClientLogger.getLogger().error("workerHealthDetector detect error:{}", e.getMessage());
            }
        }, 31000, 31000, TimeUnit.MILLISECONDS);
        registerMBean();
        ClientLogger.getLogger().info("workerHealthDetector start");
    }

    /**
     * 发送探测key
     */
    public void sendDetectKey() {
        String appName = hotCaffeineDetector.getAppName();
        // 选择一个keyRule
        KeyRule keyRule = hotCaffeineDetector.getKeyRuleCacher().selectKeyRule();
        if (keyRule == null) {
            ClientLogger.getLogger().warn("workerHealthDetector appName:{} keyRule is null", appName);
            return;
        }
        // 向所有worker发送探测
        for (String address : hotCaffeineDetector.getNettyClient().getAddresses()) {
            KeyCount keyCount = KeyCount.buildInnerKeyCount(appName, keyRule);
            try {
                Message message = sendKeyCount(address, keyCount);
                workerStatMap.computeIfAbsent(address, k -> new WorkerStat(address))
                        .addTimeConsumed(new TimeConsumed(message.getCreateTime(), keyCount.getKey()));
            } catch (Exception e) {
                ClientLogger.getLogger().warn("workerHealthDetector server:{} sendDetectKey error:{}", address,
                        e.toString());
            }
        }
    }

    /**
     * 发送keyCount
     * 
     * @param address
     * @param keyCount
     * @return
     * @throws InterruptedException
     */
    private Message sendKeyCount(String address, KeyCount keyCount) throws InterruptedException {
        List<KeyCount> list = new LinkedList<>();
        list.add(keyCount);
        Message message = new Message(hotCaffeineDetector.getAppName(), MessageType.REQUEST_NEW_KEY,
                JsonUtil.toJSON(list), System.currentTimeMillis());
        hotCaffeineDetector.getNettyClient().writeAndFlush(address, message);
        return message;
    }

    /**
     * 收到热key
     * 
     * @param keyCount
     */
    public void receiveHotKey(String address, KeyCount keyCount) {
        WorkerStat workerStat = workerStatMap.get(address);
        if (workerStat == null) {
            ClientLogger.getLogger().warn("workerHealthDetector:{} key:{} no matched request", address,
                    keyCount.getKey());
            return;
        }
        workerStat.setConsumed(keyCount.getKey());
    }

    /**
     * 检测
     */
    public void detect() {
        int unhealthyCount = 0;
        int totalCount = 0;
        // 检测
        for (String address : hotCaffeineDetector.getNettyClient().getAddresses()) {
            WorkerStat workerStat = workerStatMap.get(address);
            if (workerStat == null) {
                continue;
            }
            ++totalCount;
            if (!workerStat.healthy()) {
                ++unhealthyCount;
            }
        }
        // 移除不用的worker
        for (String address : workerStatMap.keySet()) {
            if (!hotCaffeineDetector.getNettyClient().getAddresses().contains(address)) {
                workerStatMap.remove(address);
                ClientLogger.getLogger().info("WorkerHealthDetector remove:{}", address);
            }
        }
        // 没有任何检测不健康
        if (totalCount == 0) {
            setHealthy(false);
            ClientLogger.getLogger().info("WorkerHealthDetector unhealthy! total:{}", totalCount);
            return;
        }
        // 任意worker不健康即不健康
        if (unhealthyCount > 0) {
            setHealthy(false);
            ClientLogger.getLogger().info("WorkerHealthDetector unhealthy! count:{}", unhealthyCount);
            return;
        }
        ClientLogger.getLogger().info("WorkerHealthDetector healthy! total:{}", totalCount);
        setHealthy(true);
    }

    /**
     * 关闭
     */
    public void shutdown() {
        sendKeyExecutorService.shutdown();
        detectExecutorService.shutdown();
        ClientLogger.getLogger().info("WorkerHealthDetector shutdown!");
    }

    private void registerMBean() {
        try {
            String mbeanName = "com.hotcaffeine:name=workerStat";
            ObjectName objectName = new ObjectName(mbeanName);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ClientLogger.getLogger().info("register mbean:{}", mbeanName);
            mBeanServer.registerMBean(this, objectName);
        } catch (Throwable e) {
            ClientLogger.getLogger().warn("mqmetrics mbean register error:{}", e.getMessage());
        }
    }
    
    public void setHotCaffeineDetector(HotCaffeineDetector hotCaffeineDetector) {
        this.hotCaffeineDetector = hotCaffeineDetector;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    @Override
    public Map<String, WorkerStat> getWorkerStat() {
        return workerStatMap;
    }
}
