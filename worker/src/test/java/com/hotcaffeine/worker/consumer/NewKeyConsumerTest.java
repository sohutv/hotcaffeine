package com.hotcaffeine.worker.consumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.github.benmanes.caffeine.cache.Cache;
import com.hotcaffeine.worker.consumer.NewKeyConsumer;

public class NewKeyConsumerTest {

    @Test
    public void testNotifyTwice() throws InterruptedException {
        Cache<String, AtomicInteger> hotCache = new NewKeyConsumer().getRecentCache();
        AtomicInteger counter = new AtomicInteger();
        int times = 1000;
        int threads = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        // 测试1000次
        for(int j = 0; j < times; ++j) {
            String key = "test" + j;
            // 8个任务并发
            CountDownLatch countDownLatch = new CountDownLatch(threads);
            for(int i = 0; i < threads; ++i) {
                executorService.submit(()->{
                    while(true) {
                        AtomicInteger hotCounter = hotCache.get(key, k -> new AtomicInteger());
                        // 已经执行过直接返回，保障只通知一次
                        if (hotCounter.incrementAndGet() > 1) {
                            countDownLatch.countDown();
                            return;
                        }
                        countDownLatch.countDown();
                        counter.incrementAndGet();
                    }
                });
            }
            countDownLatch.await();
        }
        Assert.assertEquals(times, counter.get());
    }

}
