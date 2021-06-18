package com.hotcaffeine.common.metric;

import java.lang.reflect.Field;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.hotcaffeine.common.util.ClientLogger;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
/**
 * netty流量
 * 
 * @author yongfeigao
 * @date 2021年3月24日
 */
public class NettyTrafficMetrics {
    private GlobalTrafficShapingHandler globalTrafficShapingHandler;

    public NettyTrafficMetrics() {
        globalTrafficShapingHandler = new GlobalTrafficShapingHandler(Executors.newSingleThreadScheduledExecutor(), 0);
        initMetrics();
    }

    public void initMetrics() {
        try {
            TrafficCounter trafficCounter = globalTrafficShapingHandler.trafficCounter();
            Field readField = TrafficCounter.class.getDeclaredField("cumulativeReadBytes");
            readField.setAccessible(true);
            AtomicLong cumulativeReadBytes = (AtomicLong) readField.get(trafficCounter);
            Field writeField = TrafficCounter.class.getDeclaredField("cumulativeWrittenBytes");
            writeField.setAccessible(true);
            AtomicLong cumulativeWrittenBytes = (AtomicLong) writeField.get(trafficCounter);
            Metrics.gauge("hotcaffeine_netty_traffic", Tags.of("group", "read"), cumulativeReadBytes);
            Metrics.gauge("hotcaffeine_netty_traffic", Tags.of("group", "write"), cumulativeWrittenBytes);
        } catch (Exception e) {
            ClientLogger.getLogger().warn("netty cannot metrics", e.toString());
        }
    }

    public GlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
        return globalTrafficShapingHandler;
    }
}
