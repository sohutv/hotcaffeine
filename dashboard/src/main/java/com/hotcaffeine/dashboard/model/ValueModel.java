package com.hotcaffeine.dashboard.model;

public class ValueModel {
    /**
     * 该热key创建时间
     */
    private long createTime;
    /**
     * 用户实际存放的value
     */
    private Object value;

    private boolean defaultValue;

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }
}
