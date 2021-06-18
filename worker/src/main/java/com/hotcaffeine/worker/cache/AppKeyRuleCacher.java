package com.hotcaffeine.worker.cache;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private Logger logger = LoggerFactory.getLogger(getClass());

    public ConcurrentMap<String, KeyRuleCacher> appKeyRuleCacherMap = new ConcurrentHashMap<>();
    
    @Autowired
    private AppCaffeineCache appCaffeineCache;

    /**
     * 更新
     * 
     * @param appName
     * @param keyRuleList
     */
    public void update(String appName, List<KeyRule> keyRuleList) {
        KeyRuleCacher keyRuleCacher = appKeyRuleCacherMap.computeIfAbsent(appName, k -> {
            return new KeyRuleCacher(k, false);
        });

        // 获取之前的keyrule
        List<KeyRule> oldKeyRuleList = keyRuleCacher.getKeyRuleList();

        keyRuleCacher.updateKeyRule(keyRuleList);

        // 只有统计间隔变了才清空缓存
        List<KeyRule> newKeyRuleList = keyRuleCacher.getKeyRuleList();
        boolean needClearCache = false;
        if (newKeyRuleList.size() != oldKeyRuleList.size()) {
            needClearCache = true;
        } else {
            outer: for (KeyRule oldKeyRule : oldKeyRuleList) {
                for (KeyRule keyRule : newKeyRuleList) {
                    if (oldKeyRule.getKey().equals(keyRule.getKey())) {
                        if (oldKeyRule.getInterval() != keyRule.getInterval()) {
                            needClearCache = true;
                            break outer;
                        }
                        break;
                    }
                }
            }
        }
        if (needClearCache) {
            logger.info("clear cache:{}", appName);
            appCaffeineCache.clearCacheByAppName(appName);
        }
    }

    public KeyRuleCacher getKeyRuleCacher(String appName) {
        return appKeyRuleCacherMap.get(appName);
    }

    public ConcurrentMap<String, KeyRuleCacher> getAppKeyRuleCacherMap() {
        return appKeyRuleCacherMap;
    }
}
