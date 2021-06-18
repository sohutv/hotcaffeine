package com.hotcaffeine.common.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.etcd.jetcd.shaded.com.google.common.eventbus.AsyncEventBus;
import io.etcd.jetcd.shaded.com.google.common.eventbus.EventBus;
import io.etcd.jetcd.shaded.com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author wuweifeng wrote on 2020-01-07
 * @version 1.0
 */
public class EventBusUtil {

    private static final EventBus eventBus = new AsyncEventBus(new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadFactoryBuilder().setNameFormat(
                    "eventBus-%d").setDaemon(true).build()));

    public static void register(Object obj) {
        eventBus.register(obj);
    }

    public static void post(Object obj) {
        eventBus.post(obj);
    }
}