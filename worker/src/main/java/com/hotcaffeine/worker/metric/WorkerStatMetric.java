package com.hotcaffeine.worker.metric;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hotcaffeine.worker.netty.processor.ClientChannelProcessor;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * worker状态统计
 * 
 * @author yongfeigao
 * @date 2021年3月26日
 */
@Component
public class WorkerStatMetric {

    @Autowired
    private ClientChannelProcessor clientChannelProcessor;

    // appName->clientSize
    private ConcurrentMap<String, AtomicLong> appClientSizeMap = new ConcurrentHashMap<>();

    /**
     * 统计客户端数量
     */
    @Scheduled(fixedRate = 5000)
    public void stat() {
        clientChannelProcessor.getClientChannelMap().forEach((appName, channelGroup) -> {
            setAppClienSize(appName, channelGroup.size());
        });
    }

    /**
     * 设置app客户端数量
     */
    public void setAppClienSize(String appName, long appSize) {
        appClientSizeMap.computeIfAbsent(appName, k -> {
            return Metrics.gauge("hotCaffeine_app", Tags.of("app", appName), new AtomicLong());
        }).set(appSize);
    }
}
