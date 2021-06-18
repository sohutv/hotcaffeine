package com.hotcaffeine.client.count;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.ClientLogger;

/**
 * 默认的key计数器
 * 
 * @author yongfeigao
 * @date 2021年1月29日
 */
public class DefaultKeyCounter {
    // 计数数组
    private ConcurrentMap<String, LongAdder>[] counterArray;

    // 每个map最大多少个元素
    private long maxSize = 10000;

    // 计数数组的下标
    private volatile int indexer = 0;

    // 是否关闭
    private volatile boolean shutdown = false;

    public DefaultKeyCounter() {
        this(2);
    }

    @SuppressWarnings("unchecked")
    public DefaultKeyCounter(int arraySize) {
        counterArray = new ConcurrentHashMap[arraySize];
        for (int i = 0; i < arraySize; ++i) {
            counterArray[i] = new ConcurrentHashMap<>();
        }
    }
    
    /**
     * 计数
     * 
     * @param sourceKey
     * @return
     */
    public long count(String key) {
        if (shutdown) {
            return 0;
        }
        // 获取value
        ConcurrentMap<String, LongAdder> countingMap = counterArray[indexer];
        LongAdder value = countingMap.get(key);
        if (value == null) {
            // 检测大小
            if (countingMap.size() < maxSize) {
                value = countingMap.computeIfAbsent(key, k -> new LongAdder());
            } else {
                ClientLogger.getLogger().warn("countingMap size >={}", maxSize);
            }
            if (value == null) {
                return 0;
            }
        }
        value.increment();
        // 计数
        return value.longValue();
    }

    /**
     * 切换
     * 
     * @return 返回前一个结果
     */
    public Collection<KeyCount> switchAndGetResult() {
        // 记录当前索引
        int curIndex = indexer;
        // 当前索引切换
        indexer = (indexer + 1) % counterArray.length;
        // 获取历史数据
        ConcurrentMap<String, LongAdder> dataMap = counterArray[curIndex];
        // 获取数据
        List<KeyCount> result = getResult(dataMap);
        // 清空历史数据
        dataMap.clear();
        return result;
    }

    /**
     * 获取结果
     * 
     * @param dataMap
     * @return
     */
    List<KeyCount> getResult(ConcurrentMap<String, LongAdder> dataMap) {
        List<KeyCount> list = new ArrayList<>(dataMap.size());
        dataMap.forEach((k, v) -> {
            KeyCount keyCount = new KeyCount();
            keyCount.setKey(k);
            keyCount.setCount(v.longValue());
            list.add(keyCount);
        });
        return list;
    }

    /**
     * 获取剩余的数据
     * 
     * @return
     */
    public List<KeyCount> getRemainedData() {
        List<KeyCount> dataList = new ArrayList<>();
        for (ConcurrentMap<String, LongAdder> concurrentMap : counterArray) {
            if (concurrentMap.size() != 0) {
                dataList.addAll(getResult(concurrentMap));
            }
        }
        return dataList;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public void shutdown() {
        this.shutdown = true;
        ClientLogger.getLogger().info("KeyCounter shutdown");
    }
}
