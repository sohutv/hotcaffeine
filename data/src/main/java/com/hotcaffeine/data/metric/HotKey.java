package com.hotcaffeine.data.metric;

/**
 * 热键
 * 
 * @author yongfeigao
 * @date 2021年3月4日
 */
public class HotKey implements Comparable<HotKey> {
    private long count;
    private int liveTime;
    private String key;
    // 调用量占比 countRate %
    private double countRate;
    // 存活时间的秒
    private transient double liveTimeSecond;
    
    public HotKey() {
    }

    public HotKey(long count, int liveTime, String key) {
        this.count = count;
        this.liveTime = liveTime;
        this.key = key;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public int getLiveTime() {
        return liveTime;
    }

    public void setLiveTime(int liveTime) {
        this.liveTime = liveTime;
    }

    public String getKey() {
        return key;
    }
    
    public static double toSecond(double time) {
        double tm = (long) (time * 10d) / 10000d;
        if (tm > 1) {
            return (long) tm;
        }
        return tm;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public double getCountRate() {
        return countRate;
    }

    public void setCountRate(double countRate) {
        this.countRate = countRate;
    }
    
    public void initCountRate(long totalCount) {
        countRate = (long) ((double) count / totalCount * 10000) / 100d;
    }

    public double getLiveTimeSecond() {
        return liveTimeSecond;
    }

    public void setLiveTimeSecond(double liveTimeSecond) {
        this.liveTimeSecond = liveTimeSecond;
    }

    @Override
    public String toString() {
        return "HotKey [count=" + count + ", liveTime=" + liveTime + ", key=" + key + "]";
    }

    @Override
    public int compareTo(HotKey hotKey) {
        int comp = (int) (count - hotKey.count);
        if (comp != 0) {
            return comp;
        }
        return (int) (liveTime - hotKey.liveTime);
    }
}