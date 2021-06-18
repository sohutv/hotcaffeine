package com.hotcaffeine.data.metric;

import java.util.List;

/**
 * 一个统计期内top的hotkey
 * 
 * @author yongfeigao
 * @date 2021年3月4日
 */
public class TopHotKey {
    // 总调用量
    private long totalCount;
    // top调用量
    private double topCount;
    // topk计算
    private TopK<HotKey> topk;
    // 临时存储
    private List<HotKey> hotKeyList;
    // topk的量
    private int topkSize;
    // 分钟
    private String minute;

    public TopHotKey() {
    }

    public TopHotKey(int topkSize) {
        this.topkSize = topkSize;
        this.topk = new TopK<>(topkSize);
    }

    public void add(HotKey hotKey) {
        topk.add(hotKey);
        totalCount += hotKey.getCount();
    }
    
    public void add(TopHotKey topHotKey) {
        this.totalCount += topHotKey.getTotalCount();
        topHotKey.getHotKeyList().forEach(hk -> {
            topk.add(hk);
        });
    }

    public void initCountRate(List<HotKey> hotKeyList) {
        for (HotKey hotKey : hotKeyList) {
            hotKey.initCountRate(totalCount);
            topCount += hotKey.getCount();
        }
    }

    public List<HotKey> buildHotKeyList() {
        return topk.getList();
    }

    public List<HotKey> getHotKeyList() {
        return hotKeyList;
    }

    public void setHotKeyList(List<HotKey> hotKeyList) {
        this.hotKeyList = hotKeyList;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public double topCountRate() {
        return (long) (topCount / totalCount * 10000) / 100d;
    }

    public double getTopCount() {
        return topCount;
    }

    public void setTopCount(double topCount) {
        this.topCount = topCount;
    }

    public int getTopkSize() {
        return topkSize;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public void setTopkSize(int topkSize) {
        this.topkSize = topkSize;
    }
}
