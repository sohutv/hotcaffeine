package com.hotcaffeine.common.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class MemoryMQTest {
    long max = 10000000;
    
    @Test
    public void test() throws InterruptedException {
        AtomicLong counter = new AtomicLong();
        MemoryMQ<String> memoryMQ = new MemoryMQ<>();
        memoryMQ.setBufferSize(10000);
        memoryMQ.setConsumerThreadNum(6);
        memoryMQ.setMinBatchDealSize(1);
        memoryMQ.setMemoryMQConsumer(str -> {
            counter.incrementAndGet();
        });
        memoryMQ.init();
        
        while(true) {
            long start = System.currentTimeMillis();
            int producers = 5;
            CountDownLatch cdl = new CountDownLatch(producers);
            for(int i = 0; i < producers; ++i) {
                new Thread(()->{
                    long index = max;
                    while (index-- > 0) {
                        memoryMQ.offer(String.valueOf(index));
                    }
                    cdl.countDown();
                }).start();
            }
            cdl.await();
            System.out.println(max / (System.currentTimeMillis() - start));
            while (memoryMQ.getBufferQueue().size() != 0) {
            }
            Thread.sleep(1000);
            System.out.println(max / (System.currentTimeMillis() - start));
            System.out.println(counter.get());
        }
    }

}
