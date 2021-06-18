package com.hotcaffeine.client;

import java.util.function.Consumer;
import java.util.function.Function;

import com.hotcaffeine.client.listener.IKeyListener;
import com.hotcaffeine.common.cache.LocalCache;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.ValueModel;
import com.hotcaffeine.common.util.ClientLogger;

/**
 * 热键缓存
 * 
 * @author yongfeigao
 * @date 2021年1月15日
 */
@SuppressWarnings({"unchecked"})
public class HotCaffeine {
    /**
     * 值获取器
     */
    private IKeyListener keyListener;
    /**
     * 热点传感器
     */
    protected HotCaffeineDetector hotCaffeineDetector;

    /**
     * ruleKey
     */
    private String ruleKey;

    public HotCaffeine(String ruleKey, IKeyListener hotCaffeineListener, HotCaffeineDetector hotCaffeineDetector) {
        this.ruleKey = ruleKey;
        this.keyListener = hotCaffeineListener;
        this.hotCaffeineDetector = hotCaffeineDetector;
    }

    /**
     * 获取key对应的值，自动检测是否为热点。 
     * 当如下情况时，function获取对应的值: 
     * 1.key无对应的规则 
     * 2.key非热点
     * 3.key是热点，但是无值
     * @param key
     * @param function 未命中hotCaffeine或hotCaffeine中无值回调该函数获取值
     * @return T 如果function返回值为null，则返回null
     */
    public <T> T getValue(String key, Function<String, T> function) {
        return getValue(key, function, null);
    }

    /**
     * 获取key对应的值，自动检测是否为热点。 
     * 当如下情况时，function获取对应的值: 
     * 1.key无对应的规则 
     * 2.key非热点
     * 3.key是热点，但是无值
     * @param key
     * @param function 未命中hotCaffeine或hotCaffeine中无值回调该函数获取值
     * @param hitCallbackConsumer 命中hotCaffeine且hotCaffeine中有值回调该函数
     * @return T 如果function返回值为null，则返回null
     */
    public <T> T getValue(String key, Function<String, T> function, Consumer<String> hitCallbackConsumer) {
        ValueModel valueModel = getValueModel(key);
        if (valueModel == null) {
            return function.apply(key);
        }
        if (valueModel.isDefaultValue()) {
            // 默认值重设
            valueModel.setValue(function.apply(key));
        } else if (hitCallbackConsumer != null) {
            hitCallbackConsumer.accept(key);
        }
        return (T) valueModel.getValue();
    }

    /**
     * 是否是热点key，自动检测是否为热点。
     * 
     * @param key
     * @return 空key返回false
     */
    public boolean isHot(String key) {
        return getValueModel(key) != null;
    }

    /**
     * 获取ValueModel，自动检测是否为热点。
     * 
     * @param key
     * @return 空key返回null
     */
    public ValueModel getValueModel(String key) {
        if (key == null || key.length() == 0) {
            return null;
        }
        if (key.length() >= hotCaffeineDetector.getMaxKeyLength()) {
            ClientLogger.getLogger().warn("key:{} is too long to collect!", key);
            return null;
        }
        try {
            KeyRule keyRule = finKeyRule(key);
            // 没有配置规则不进行检测
            if (keyRule == null) {
                return null;
            }
            LocalCache<ValueModel> localCache = hotCaffeineDetector.getKeyRuleCacher().getCache(keyRule);
            if (localCache == null) {
                return null;
            }
            ValueModel value = localCache.get(key);
            // 非热点
            if (value == null) {
                count(keyRule, key);
                return null;
            }
            long now = System.currentTimeMillis();
            // 空缓存过期判断
            if (value.isNullValueExprie(keyRule.getNullValueExpire() * 1000, now)) {
                count(keyRule, key);
                return null;
            }
            // 临近过期了计数
            if (value.isNearExpire(now)) {
                count(keyRule, key);
            }
            return value;
        } catch (Throwable e) { // 发生任何异常不能影响客户端
            ClientLogger.getLogger().error("getValueModel key:{} error:{}", key, e.toString());
            return null;
        }
    }

    protected KeyRule finKeyRule(String key) {
        return hotCaffeineDetector.getKeyRuleCacher().getRule(ruleKey);
    }

    /**
     * 设置热点数据值
     * 
     * @param key
     * @param newValue
     * @return 是热点，并且是默认值才返回成功
     */
    public boolean setHotValue(String key, Object newValue) {
        if (key == null || key.length() == 0 || newValue == null) {
            return false;
        }
        try {
            KeyRule keyRule = finKeyRule(key);
            // 没有配置规则不进行检测
            if (keyRule == null) {
                return false;
            }
            LocalCache<ValueModel> localCache = hotCaffeineDetector.getKeyRuleCacher().getCache(keyRule);
            if (localCache == null) {
                return false;
            }
            ValueModel value = localCache.get(key);
            if (value == null) {
                return false;
            }
            value.setValue(newValue);
        } catch (Throwable e) { // 发生任何异常不能影响客户端
            ClientLogger.getLogger().error("setHotValue key:{} error:{}", key, e.toString());
            return false;
        }
        return true;
    }

    /**
     * 删除key
     * 
     * @param key
     * @return
     */
    public boolean remove(String key) {
        if (key == null || key.length() == 0) {
            return true;
        }
        try {
            KeyRule keyRule = finKeyRule(key);
            // 没有配置规则不进行检测
            if (keyRule == null) {
                return false;
            }
            LocalCache<ValueModel> localCache = hotCaffeineDetector.getKeyRuleCacher().getCache(keyRule);
            if (localCache == null) {
                return false;
            }
            localCache.delete(key);
            return true;
        } catch (Throwable e) { // 发生任何异常不能影响客户端
            ClientLogger.getLogger().error("remove key:{} error:{}", key, e.toString());
            return false;
        }
    }

    /**
     * 统计数量
     * 
     * @param key
     */
    private void count(KeyRule keyRule, String key) {
        String fullKey = keyRule.buildFullKey(key);
        long count = hotCaffeineDetector.getKeyCounter().count(fullKey);
        // 本地热key检测
        if (keyRule.isEnableLocalDetector()) {
            hotCaffeineDetector.localDetect(fullKey, count, keyRule.getThreshold(), keyRule.getDestQps());
        }
    }

    public IKeyListener getKeyListener() {
        return keyListener;
    }
}
