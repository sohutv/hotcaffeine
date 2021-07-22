package com.hotcaffeine.common.model;

import com.hotcaffeine.common.util.IpUtil;

import io.netty.channel.Channel;

/**
 * 热key的定义
 * 
 * @author wuweifeng wrote on 2019-12-05
 * @version 1.0
 */
public class KeyCount {
    // 内部key计数器
    public static long innerKeyCounter;
    
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
    
    // 是否是内部key
    private boolean isInner;
    
    private transient Channel channel;

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
    
    public static KeyCount buildInnerKeyCount(String appName, KeyRule keyRule) {
        KeyCount keyCount = new KeyCount();
        keyCount.setAppName(appName);
        keyCount.setCount(keyRule.getThreshold());
        keyCount.setInner(true);
        String key = IpUtil.INSTANCE_ID + ":" + (innerKeyCounter++);
        if (keyRule.isPrefix()) {
            keyCount.setKey(keyRule.getKey() + ":" + key);
        } else {
            keyCount.setKey(KeyRule.buildFullKey(keyRule.getKey(), key));
        }
        return keyCount;
    }

    public boolean isInner() {
        return isInner;
    }

    public void setInner(boolean isInner) {
        this.isInner = isInner;
    }
    
    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

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
}
