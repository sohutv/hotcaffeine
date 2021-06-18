package com.hotcaffeine.common.util;

import io.micrometer.core.instrument.Metrics;

/**
 * 统计
 * 
 * @author yongfeigao
 * @date 2021年3月26日
 */
public class MetricsUtil {
    /**
     * 增加netty接收次数
     */
    public static void incrReceiveTimes() {
        Metrics.counter("hotcaffeine_receive_times").increment();
    }

    /**
     * 增加接收key量
     * 
     * @param count
     */
    public static void incrReceiveKeys(double count) {
        Metrics.counter("hotcaffeine_receive_keys").increment(count);
    }

    /**
     * 增加处理key量
     * 
     * @param count
     */
    public static void incrDealKeys() {
        Metrics.counter("hotcaffeine_deal_keys").increment();
    }

    /**
     * 增加发送key量
     */
    public static void incrSendKeys() {
        incrSendKeys(1);
    }
    
    /**
     * 增加发送key量
     */
    public static void incrSendKeys(double count) {
        Metrics.counter("hotcaffeine_send_keys").increment(count);
    }
}
