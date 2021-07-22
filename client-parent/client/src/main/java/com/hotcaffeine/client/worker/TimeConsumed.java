package com.hotcaffeine.client.worker;

/**
 * 时间消耗
 * 
 * @author yongfeigao
 * @date 2021年7月21日
 */
public class TimeConsumed {
    // 发送的key
    private String key;
    // 开始时间
    private long start;
    // 消耗时间
    private long consumed = -1;
    // 是否检测过
    private boolean detected;

    public TimeConsumed() {
    }

    public TimeConsumed(long start, String key) {
        this.start = start;
        this.key = key;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getConsumed() {
        return consumed;
    }

    public void setConsumed(long consumed) {
        this.consumed = consumed;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isDetected() {
        return detected;
    }

    public void setDetected(boolean detected) {
        this.detected = detected;
    }
}
