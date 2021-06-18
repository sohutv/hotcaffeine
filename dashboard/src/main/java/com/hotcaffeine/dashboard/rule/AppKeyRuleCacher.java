package com.hotcaffeine.dashboard.rule;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;

/**
 * AppKeyRuleCacher
 * 
 * @author yongfeigao
 * @date 2021年3月19日
 */
@Component
public class AppKeyRuleCacher {
    public ConcurrentMap<String, KeyRuleCacher> appKeyRuleCacherMap = new ConcurrentHashMap<>();

    /**
     * 更新
     * @param appName
     * @param keyRuleList
     */
    public void update(String appName, List<KeyRule> keyRuleList) {
        appKeyRuleCacherMap.computeIfAbsent(appName, k -> {
            return new KeyRuleCacher(k, false);
        }).updateKeyRule(keyRuleList);
    }
    
    public void updateKeyCache(String appName, List<CacheRule> keyCacheList) {
        appKeyRuleCacherMap.computeIfAbsent(appName, k -> {
            return new KeyRuleCacher(k, false);
        }).updateCacheRule(keyCacheList, false);
    }

    public KeyRuleCacher getKeyRuleCacher(String appName) {
        return appKeyRuleCacherMap.get(appName);
    }

    public ConcurrentMap<String, KeyRuleCacher> getAppKeyRuleCacherMap() {
        return appKeyRuleCacherMap;
    }
}
