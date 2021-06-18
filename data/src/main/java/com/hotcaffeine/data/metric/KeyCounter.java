package com.hotcaffeine.data.metric;

public class KeyCounter implements Comparable<KeyCounter> {
    // 调用量
    private int counter;
    // key量
    private long keyCount;
    // key量的比率
    private double keyCountRate;
    // 生存时间
    private long survivalTime;
    
    private long maxSurvivalTime;
    
    private long minSurvivalTime = Long.MAX_VALUE;

    public KeyCounter() {
    }
    
    public KeyCounter(int counter, long keyCount) {
        this.counter = counter;
        this.keyCount = keyCount;
    }
    
    public void add(KeyCounter keyCounter) {
        this.keyCount += keyCounter.getKeyCount();
        this.survivalTime += keyCounter.getSurvivalTime();
        if (this.maxSurvivalTime < keyCounter.getMaxSurvivalTime()) {
            this.maxSurvivalTime = keyCounter.getMaxSurvivalTime();
        }
        if (this.minSurvivalTime > keyCounter.getMinSurvivalTime()) {
            this.minSurvivalTime = keyCounter.getMinSurvivalTime();
        }
    }

    public KeyCounter incr() {
        ++keyCount;
        return this;
    }
    
    public KeyCounter addSurvivalTime(int survivalTime) {
        this.survivalTime += survivalTime;
        if (survivalTime > maxSurvivalTime) {
            maxSurvivalTime = survivalTime;
        }
        if (survivalTime < minSurvivalTime) {
            minSurvivalTime = survivalTime;
        }
        return this;
    }

    @Override
    public int compareTo(KeyCounter o) {
        return (int) (o.keyCount * o.counter - keyCount * counter);
    }

    public static double toSecond(double time) {
        double tm = (long) (time * 10d) / 10000d;
        if (tm > 1) {
            return (long) tm;
        }
        return tm;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + counter;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        KeyCounter other = (KeyCounter) obj;
        if (counter != other.counter)
            return false;
        return true;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public long getKeyCount() {
        return keyCount;
    }

    public void setKeyCount(long keyCount) {
        this.keyCount = keyCount;
    }

    public double keyCountRate() {
        return keyCountRate;
    }

    public void initKeyCountRate(double totalCount) {
        keyCountRate = ((long) (keyCount * counter / totalCount * 10000)) / 10000D;
    }

    public long getSurvivalTime() {
        return survivalTime;
    }

    public void setSurvivalTime(long survivalTime) {
        this.survivalTime = survivalTime;
    }

    public long getMaxSurvivalTime() {
        return maxSurvivalTime;
    }

    public void setMaxSurvivalTime(long maxSurvivalTime) {
        this.maxSurvivalTime = maxSurvivalTime;
    }

    public long getMinSurvivalTime() {
        return minSurvivalTime;
    }

    public void setMinSurvivalTime(long minSurvivalTime) {
        this.minSurvivalTime = minSurvivalTime;
    }
}
