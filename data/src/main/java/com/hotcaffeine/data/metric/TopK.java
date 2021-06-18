package com.hotcaffeine.data.metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * topk实现
 * 
 * @author yongfeigao
 * @date 2021年2月25日
 * @param <E>
 */
public class TopK<E extends Comparable<E>> {
    private PriorityQueue<E> queue;
    private int maxSize; // 堆的最大容量

    public TopK(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalStateException();
        }
        this.maxSize = maxSize;
        this.queue = new PriorityQueue<>(maxSize + 1);
    }

    public void add(E e) {
        queue.add(e);
        if (queue.size() > maxSize) {
            queue.poll();
        }
    }

    /**
     * 获取topk列表
     * @return
     */
    public List<E> getList() {
        List<E> list = new ArrayList<>();
        for (E e : queue) {
            list.add(e);
        }
        Collections.sort(list, (o1, o2) -> {
            return o2.compareTo(o1);
        });
        return list;
    }

    @Override
    public String toString() {
        return "TopK [queue=" + getList() + ", maxSize=" + maxSize + "]";
    }
}
