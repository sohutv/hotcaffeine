package com.hotcaffeine.common.model;

/**
 * @author wuweifeng
 * @version 1.0
 * @date 2020-07-07
 */
public class ValueModel {
    /**
     * 该热key创建时间
     */
    private long createTime = System.currentTimeMillis();
    
    public static final long EXPIRE_TIME = 2000;
    // null对象
    public static final Object NULL_OBJECT = new Object();
    /**
     * 本地缓存时间，单位毫秒
     */
    private int duration;
    /**
     * 用户实际存放的value
     */
    private Object value = NULL_OBJECT;

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public boolean isDefaultValue() {
        return NULL_OBJECT == value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long leftTime(long now) {
        return getCreateTime() + getDuration() - now;
    }

    public boolean isLeftTimeNearExpire(long leftTime) {
        return leftTime <= EXPIRE_TIME;
    }
    
    public boolean isNearExpire(long now) {
        return isLeftTimeNearExpire(leftTime(now));
    }

    /**
     * 空值是否过期
     * @param exprieTime
     * @param now
     * @return
     */
    public boolean isNullValueExprie(long exprieTime, long now) {
        if (value != null) {
            return false;
        }
        if (exprieTime <= 0) {
            return false;
        }
        return now - getCreateTime() >= exprieTime;
    }
}
