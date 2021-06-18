package com.hotcaffeine.dashboard.model;


import java.io.Serializable;
import java.util.Date;

public class KeyTimely implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String key;

    private String appName;

    /**
     * 热key所属的appName下某个规则key
     */
    private String rule;

    /**
     * 实际的热key
     */
    private String realHotKey;

    private String val;

    /**
     * 缓存时间
     */
    private Long duration;

    private Date createTime;

    /**
     * 该rule的描述
     */
    private transient String ruleDesc;


    private String updater;


    public KeyTimely() {
    }

    public KeyTimely(String key, String updater) {
        this.key = key;
        this.updater = updater;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getRealHotKey() {
        return realHotKey;
    }

    public void setRealHotKey(String realHotKey) {
        this.realHotKey = realHotKey;
    }

    public String getVal() {
        return val;
    }

    public void setVal(String val) {
        this.val = val;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRuleDesc() {
        return ruleDesc;
    }

    public void setRuleDesc(String ruleDesc) {
        this.ruleDesc = ruleDesc;
    }

    public String getUpdater() {
        return updater;
    }

    public void setUpdater(String updater) {
        this.updater = updater;
    }

    @Override
    public String toString() {
        return "KeyTimely{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", appName='" + appName + '\'' +
                ", rule='" + rule + '\'' +
                ", realHotKey='" + realHotKey + '\'' +
                ", val='" + val + '\'' +
                ", duration=" + duration +
                ", createTime=" + createTime +
                ", ruleDesc='" + ruleDesc + '\'' +
                ", updater='" + updater + '\'' +
                '}';
    }

    public static Builder aKeyTimely() {
        return new Builder();
    }

    public static final class Builder {
        private String key;
        private String appName;
        private String rule;
        private String realHotKey;
        private String val;
        private Long duration;
        private Date createTime;
        private transient String ruleDesc;
        private String updater;

        private Builder() {
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder rule(String rule) {
            this.rule = rule;
            return this;
        }

        public Builder realHotKey(String realHotKey) {
            this.realHotKey = realHotKey;
            return this;
        }

        public Builder val(String val) {
            this.val = val;
            return this;
        }

        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }

        public Builder createTime(Date createTime) {
            this.createTime = createTime;
            return this;
        }

        public Builder ruleDesc(String ruleDesc) {
            this.ruleDesc = ruleDesc;
            return this;
        }

        public Builder updater(String updater) {
            this.updater = updater;
            return this;
        }

        public KeyTimely build() {
            KeyTimely keyTimely = new KeyTimely();
            keyTimely.setKey(key);
            keyTimely.setAppName(appName);
            keyTimely.setRule(rule);
            keyTimely.setRealHotKey(realHotKey);
            keyTimely.setVal(val);
            keyTimely.setDuration(duration);
            keyTimely.setCreateTime(createTime);
            keyTimely.setRuleDesc(ruleDesc);
            keyTimely.setUpdater(updater);
            return keyTimely;
        }
    }
}