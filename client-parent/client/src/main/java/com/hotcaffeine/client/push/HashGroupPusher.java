package com.hotcaffeine.client.push;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.hotcaffeine.client.count.DefaultKeyCounter;
import com.hotcaffeine.client.netty.NettyClient;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.MetricsUtil;

import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * 哈希分组push数据到server端
 * 
 * @author yongfeigao
 * @date 2021年1月15日
 */
public class HashGroupPusher {
    // push间隔
    private long pushInterval;

    // key计数器
    private DefaultKeyCounter keyCounter;

    // key类型
    private MessageType type;

    // app name
    private String appName;

    // netty
    private NettyClient nettyClient;
    
    // 任务执行
    private ScheduledExecutorService executorService;

    /**
     * 启动push
     */
    public void start() {
        executorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
                "hotcaffeine-"+type+"-send-" + appName + "-%d").setDaemon(true).build());
        executorService.scheduleAtFixedRate(() -> {
            try {
                push(keyCounter.switchAndGetResult());
            } catch (Exception e) {
                ClientLogger.getLogger().error(type + " error", e);
            }
        }, 0, pushInterval, TimeUnit.MILLISECONDS);
        ClientLogger.getLogger().info("type:{} pushInterval:{} start", type, pushInterval);
    }

    /**
     * push数据
     * 
     * @param list
     */
    public void push(Collection<KeyCount> list) {
        if (list.size() == 0) {
            return;
        }
        // worker不可达不再推送
        if (nettyClient.isWorkerUnreachable()) {
            ClientLogger.getLogger().warn("worker is unreachable, cannot push:{}", list.size());
            return;
        }
        MetricsUtil.incrSendKeys(list.size());
        Map<String, List<KeyCount>> map = new HashMap<>();
        for (KeyCount keyCount : list) {
            String server = nettyClient.choose(keyCount.getKey());
            if (server == null) {
                continue;
            }
            List<KeyCount> newList = map.computeIfAbsent(server, k -> new ArrayList<>());
            newList.add(keyCount);
        }
        for (String server : map.keySet()) {
            try {
                List<KeyCount> kl = map.get(server);
                nettyClient.writeAndFlush(server, new Message(appName, type, JsonUtil.toJSON(kl),
                        System.currentTimeMillis()));
                if (ClientLogger.getLogger().isDebugEnabled()) {
                    ClientLogger.getLogger().debug("type:{} send to server:{} size:{}", type, server, kl.size());
                }
            } catch (Exception e) {
                ClientLogger.getLogger().error("{} flush error", server, e);
            }
        }
    }

    /**
     * 关闭
     */
    public void shutdown() {
        executorService.shutdown();
        // 等待安全关闭
        while (!executorService.isTerminated()) {
            try {
                ClientLogger.getLogger().info("type:{} shutting down!", type);
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                ClientLogger.getLogger().info("type:{} shutdown InterruptedException", type);
            }
        }
        // 发送剩余的数据
        push(keyCounter.getRemainedData());
        ClientLogger.getLogger().info("type:{} shutdown!", type);
    }

    public long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(long pushInterval) {
        this.pushInterval = pushInterval;
    }

    public void setKeyCollector(DefaultKeyCounter keyCounter) {
        this.keyCounter = keyCounter;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public void setNettyClient(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
