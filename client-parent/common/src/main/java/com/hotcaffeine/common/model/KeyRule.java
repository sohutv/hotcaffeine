package com.hotcaffeine.common.model;

/**
 * @author wuweifeng wrote on 2020-02-26
 * @version 1.0
 */
public class KeyRule {
    public static final String SPLITER = "@@";
    
    public static final String DEFAULT_KEY = "*";

    private String key;
    // 是否是前缀匹配
    private boolean prefix;
    /**
     * 间隔时间（秒）
     */
    private int interval;
    /**
     * 累计数量
     */
    private int threshold;
    
    // 开启本地检测
    private boolean enableLocalDetector;
    
    /**
     * 描述
     */
    private String desc;
    
    // 目标qps
    private double destQps = Double.MAX_VALUE;
    
    /**
     * 统计topk的数量
     */
    private int topkCount = 100;
    
    // 是否使用topk作为热点key
    private boolean useTopKAsHotKey;
    // 缓存名
    private String cacheName;
    
    // 默认启用该规则
    private boolean disabled;
    
    // 空值过期时间
    private int nullValueExpire;

    public boolean isPrefix() {
        return prefix;
    }

    public void setPrefix(boolean prefix) {
        this.prefix = prefix;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnableLocalDetector() {
        return enableLocalDetector;
    }

    public String getDesc() {
        return desc;
    }

    public int getInterval() {
        return interval;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setEnableLocalDetector(boolean enableLocalDetector) {
        this.enableLocalDetector = enableLocalDetector;
    }

    public void setTopkCount(int topkCount) {
        this.topkCount = topkCount;
    }

    public void setUseTopKAsHotKey(boolean useTopKAsHotKey) {
        this.useTopKAsHotKey = useTopKAsHotKey;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void initDestQps() {
        if (interval > 0) {
            this.destQps = (double) threshold / interval;
        }
    }

    public double getDestQps() {
        return destQps;
    }

    public int getTopkCount() {
        return topkCount;
    }

    public boolean isUseTopKAsHotKey() {
        return useTopKAsHotKey;
    }
    
    public boolean isDefault() {
        return DEFAULT_KEY.equals(key);
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }
    
    /**
     * 是否是完整key
     * @param key
     * @return
     */
    public static boolean isFullRule(String key) {
        return key.indexOf(SPLITER) != -1;
    }
    
    /**
     * 构建完整key：ruleKey + @@ + key
     * @param key
     * @return
     */
    public String buildFullKey(String key) {
        if (prefix) {
            return key;
        }
        return buildFullKey(this.key, key);
    }
    
    public String stripRuleKey(String key) {
        if (prefix) {
            return key;
        }
        String[] tmp = key.split(SPLITER, 2);
        if(tmp.length == 2) {
            return tmp[1];
        }
        return key;
    }
    
    public static String buildFullKey(String ruleKey, String key) {
        if (ruleKey == null || ruleKey.length() == 0) {
            return key;
        }
        return ruleKey + SPLITER + key;
    }

    public int getNullValueExpire() {
        return nullValueExpire;
    }

    public void setNullValueExpire(int nullValueExpire) {
        this.nullValueExpire = nullValueExpire;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((cacheName == null) ? 0 : cacheName.hashCode());
        result = prime * result + (disabled ? 1231 : 1237);
        result = prime * result + (enableLocalDetector ? 1231 : 1237);
        result = prime * result + interval;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + nullValueExpire;
        result = prime * result + (prefix ? 1231 : 1237);
        result = prime * result + threshold;
        result = prime * result + topkCount;
        result = prime * result + (useTopKAsHotKey ? 1231 : 1237);
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
        KeyRule other = (KeyRule) obj;
        if (cacheName == null) {
            if (other.cacheName != null)
                return false;
        } else if (!cacheName.equals(other.cacheName))
            return false;
        if (disabled != other.disabled)
            return false;
        if (enableLocalDetector != other.enableLocalDetector)
            return false;
        if (interval != other.interval)
            return false;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (nullValueExpire != other.nullValueExpire)
            return false;
        if (prefix != other.prefix)
            return false;
        if (threshold != other.threshold)
            return false;
        if (topkCount != other.topkCount)
            return false;
        if (useTopKAsHotKey != other.useTopKAsHotKey)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "KeyRule [key=" + key + ", prefix=" + prefix + ", interval=" + interval + ", threshold=" + threshold
                + ", enableLocalDetector=" + enableLocalDetector + ", desc=" + desc + ", destQps=" + destQps
                + ", topkCount=" + topkCount + ", useTopKAsHotKey=" + useTopKAsHotKey + ", cacheName=" + cacheName
                + ", disabled=" + disabled + ", nullValueExpire=" + nullValueExpire + "]";
    }
}
