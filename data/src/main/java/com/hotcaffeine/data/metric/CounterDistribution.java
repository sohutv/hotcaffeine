package com.hotcaffeine.data.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用量分布
 * 
 * @author yongfeigao
 * @date 2021年2月8日
 */
public class CounterDistribution {

    // 最多记多少个分布
    private int maxSize = 10000;

    // key：调用量的预估值，value：key的数量
    private Map<Integer, KeyCounter> distributionMap = new HashMap<>();

    // 总调用量
    private double totalCount = 0;

    /**
     * 计数
     * 
     * @param counter
     * @return 满了返回true
     */
    public boolean incr(long counter) {
        return incr(counter, 0);
    }

    /**
     * 计数
     * 
     * @param counter
     * @return 满了返回true
     */
    public boolean incr(long counter, int survivalTime) {
        // 计算hash
        int hash = hash(counter);
        distributionMap.computeIfAbsent(hash, k -> new KeyCounter(k, 0)).incr().addSurvivalTime(survivalTime);
        totalCount += hash;
        return distributionMap.size() > maxSize;
    }

    /**
     * hash方法 
     * [1,100]共100个 
     * (100,500]共250个 
     * (500,1000]共50个 
     * (1000,+∞]每100个算1个
     * 
     * @param counter
     * @return
     */
    public int hash(long counter) {
        if (counter <= 0) {
            throw new IllegalArgumentException("counter:" + counter);
        }
        // 小于100的，准确计数
        if (counter <= 100) {
            return (int) counter;
        }
        // (100, 500]以5为间隔，比如[101,105]都记做105，(105,110]都记做110
        if (counter <= 500) {
            int left = (int) (counter % 10);
            if (left >= 1 && left <= 5) {
                left = 5;
            } else {
                left = 0;
            }
            int tmp = (int) (Math.ceil(counter / 10D) * 10);
            return tmp - left;
        }
        // (500, 1000]以10为间隔，比如[501,510]都记做110
        if (counter <= 1000) {
            return (int) (Math.ceil(counter / 10D) * 10);
        }
        // 大于1000的以100为间隔
        return (int) (Math.ceil(counter / 100D) * 100);
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public Map<Integer, KeyCounter> getDistributionMap() {
        return distributionMap;
    }

    public double getTotalCount() {
        return totalCount;
    }
}
