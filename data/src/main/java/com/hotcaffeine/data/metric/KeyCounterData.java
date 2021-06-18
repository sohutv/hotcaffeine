package com.hotcaffeine.data.metric;

import java.util.Map;

public class KeyCounterData {
    private String minute;
    // 总调用量
    private double totalCount = 0;

    private Map<Integer, KeyCounter> distributionMap;

    public KeyCounterData(String minute) {
        this.minute = minute;
    }

    public void add(KeyCounterData keyCounterData) {
        this.totalCount += keyCounterData.getTotalCount();
        if (distributionMap == null) {
            distributionMap = keyCounterData.getDistributionMap();
        } else {
            for (Integer key : keyCounterData.getDistributionMap().keySet()) {
                KeyCounter newKeyCounter = keyCounterData.getDistributionMap().get(key);
                KeyCounter existKeyCounter = distributionMap.get(key);
                if (existKeyCounter == null) {
                    distributionMap.put(key, newKeyCounter);
                } else {
                    existKeyCounter.add(newKeyCounter);
                }
            }
        }
    }

    public KeyCounterData(Map<Integer, KeyCounter> distributionMap, double totalCount, String minute) {
        this.minute = minute;
        this.totalCount = totalCount;
        this.distributionMap = distributionMap;
    }

    public void initKeyCountRate() {
        distributionMap.forEach((k, v) -> {
            v.initKeyCountRate(totalCount);
        });
    }

    public void setTotalCount(double totalCount) {
        this.totalCount = totalCount;
    }

    public void addTotalCount(double totalCount) {
        this.totalCount += totalCount;
    }

    public double getTotalCount() {
        return totalCount;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public Map<Integer, KeyCounter> getDistributionMap() {
        return distributionMap;
    }

    public void setDistributionMap(Map<Integer, KeyCounter> distributionMap) {
        this.distributionMap = distributionMap;
    }
}
