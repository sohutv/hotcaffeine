package com.hotcaffeine.common.util;

import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.util.MemoryMQ.MemoryMQConsumer;

/**
 * MemoryMQ组
 * 
 * @author yongfeigao
 * @date 2018年12月14日
 */
public class MemoryMQGroup<T> implements Destroyable {
    // MemoryMQ数组
    private MemoryMQ<T>[] memoryMQArray;
    // 数组大小
    private int size;

    private IGroupBy<T> groupBy;

    @SuppressWarnings("unchecked")
    public MemoryMQGroup(IGroupBy<T> groupBy, int size, int bufferSize, String consumerNamePrefix,
            MemoryMQConsumer<T> memoryMQConsumer) {
        this.groupBy = groupBy;
        this.size = size;
        memoryMQArray = new MemoryMQ[size];
        for (int i = 0; i < memoryMQArray.length; ++i) {
            memoryMQArray[i] = new MemoryMQ<T>();
            // 最大缓存量
            memoryMQArray[i].setBufferSize(bufferSize);
            // 最小批量处理数量
            memoryMQArray[i].setMinBatchDealSize(1);
            memoryMQArray[i].setConsumerName(consumerNamePrefix + "_" + i);
            memoryMQArray[i].setMemoryMQConsumer(memoryMQConsumer);
            memoryMQArray[i].setConsumerThreadNum(1);
            memoryMQArray[i].setDestroyOrder(30);
            memoryMQArray[i].init();
        }
    }

    /**
     * 生产对象，无可用空间会一直等待
     * 
     * @param t
     * @throws InterruptedException
     */
    public void put(T t) throws Exception {
        memoryMQArray[index(t)].put(t);
    }

    /**
     * 生产对象，无可用空间直接返回
     * 
     * @param t
     * @throws InterruptedException
     */
    public boolean offer(T t) {
        return memoryMQArray[index(t)].offer(t);
    }

    private int index(T t) {
        long group = groupBy.groupValue(t);
        return (int) (group % size);
    }

    public void shutdown() throws InterruptedException {
        for (MemoryMQ<T> memoryMQ : memoryMQArray) {
            memoryMQ.shutdown();
        }
    }

    /**
     * 根据值来分组
     * 
     * @author yongfeigao
     * @date 2019年1月28日
     * @param <T>
     */
    public interface IGroupBy<T> {

        /**
         * 分组的值
         * 
         * @param t
         * @return
         */
        public long groupValue(T t);
    }
    
    @Override
    public int compareTo(Destroyable o) {
        return order() - o.order();
    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }

    @Override
    public int order() {
        return 30;
    }
}
