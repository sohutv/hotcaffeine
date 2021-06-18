package com.hotcaffeine.worker.netty.request;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/**
 * 同步请求
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 * @param <T>
 */
public class SyncRequest<T> {
    private CountDownLatch latch = new CountDownLatch(1);
    
    private T response;

    public T get(long timeout, TimeUnit unit) throws InterruptedException {
        if (latch.await(timeout, unit)) {
            return this.response;
        }
        return null;
    }

    public void setResponse(T response) {
        this.response = response;
        latch.countDown();
    }
}
