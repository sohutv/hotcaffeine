package com.hotcaffeine.common.model;

/**
 * 热key的定义
 * 
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class KeyCount {
    /**
     * 创建的时间
     */
    private long createTime;
    /**
     * key的名字
     */
    private String key;
    /**
     * 该key出现的数量，如果一次一发那就是1，累积多次发那就是count
     */
    private long count;

    /**
     * 来自于哪个应用
     */
    private String appName;
    /**
     * 是否是删除事件
     */
    private boolean remove;

    @Override
    public String toString() {
        return "keyCount{" +
                "createTime=" + createTime +
                ", key='" + key + '\'' +
                ", count=" + count +
                ", appName='" + appName + '\'' +
                ", remove=" + remove +
                '}';
    }

    public boolean isRemove() {
        return remove;
    }

    public void setRemove(boolean remove) {
        this.remove = remove;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
    public String uniqueKey() {
        return appName + key;
    }
}
