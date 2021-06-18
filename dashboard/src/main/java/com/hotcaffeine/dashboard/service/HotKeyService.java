package com.hotcaffeine.dashboard.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;
import com.hotcaffeine.data.store.IRedis;

/**
 * 热key服务
 * 
 * @author yongfeigao
 * @date 2021年5月31日
 */
@Component
public class HotKeyService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String HOT_KEY = "hk:";

    @Autowired
    private IRedis redis;

    private int maxSize = 10000; // 每个集合最大数量,暂时写死

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    /**
     * 热key存入redis
     */
    public void putKeyCount(KeyCount keyCount) {
        if (keyCount == null || keyCount.getCreateTime() <= 0L) {
            return;
        }
        String appName = keyCount.getAppName();
        // 根据当前热key匹配对应的规则keyRule
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(appName);
        KeyRule keyRule = keyRuleCacher.findRule(keyCount.getKey());
        if (keyRule == null) {
            logger.error("rule is null, appName:{}, ruleKey:{}", appName, keyCount.getKey());
            return;
        }
        CacheRule keyCache = keyRuleCacher.getCacheRule(keyRule);
        if (keyCache == null) {
            return;
        }
        // 存入缓存
        redis.zadd(buildSortedSetKey(appName, keyRule.getKey()), keyCount.getCreateTime(), JSON.toJSONString(keyCount));
    }

    /**
     * 根据appName和其某个规则查询热key索引列表
     */
    public List<KeyCount> getKeyCountList(String appName, String rule) {
        Set<String> result = redis.zrevrange(buildSortedSetKey(appName, rule), 0, -1);
        if (result == null) {
            return null;
        }
        return result.stream().map(json -> {
            try {
                return JSON.parseObject(json, KeyCount.class);
            } catch (Exception e) {
                logger.warn("parse json:{}", json, e.toString());
                throw e;
            }
        }).collect(Collectors.toList());
    }

    /**
     * 查询某个appName下具体的热key
     */
    public List<KeyCount> getKeyCount(String appName, String rule, String key) {
        List<KeyCount> list = getKeyCountList(appName, rule);
        if (list == null) {
            return null;
        }
        return list.stream().filter(keyCount -> keyCount.getKey().contains(key)).collect(Collectors.toList());
    }

    /**
     * 删除热key，同时删除实际数据和索引
     */
    public boolean removeKeyCount(String appName, String rule, String key) {
        Set<String> result = redis.zrevrange(buildSortedSetKey(appName, rule), 0, -1);
        if (result == null) {
            return false;
        }
        Optional<String> optional = result.stream().filter(json -> {
            KeyCount count = JSON.parseObject(json, KeyCount.class);
            return count.getKey().equals(key);
        }).findFirst();
        optional.ifPresent(json -> {
            redis.zrem(buildSortedSetKey(appName, rule), json);
        });
        return true;
    }

    // 清除旧数据
    public void cleanUp() {
        appKeyRuleCacher.getAppKeyRuleCacherMap().forEach((appName, appKeyRuleCacher) -> {
            appKeyRuleCacher.getKeyRuleList().forEach(keyRule -> {
                String key = buildSortedSetKey(appName, keyRule.getKey());
                long size = redis.zcard(key);
                if (size > maxSize) { // 超出容量了，这里实际可以取keyRule里的配置
                    redis.zremrangeByRank(key, 0, size - maxSize - 1);
                }
            });
        });
    }

    /**
     * keyCount存储到有序集合对应的key：appName + KeyRule.key
     */
    public String buildSortedSetKey(String appName, String keyForKeyRule) {
        return HOT_KEY + appName + keyForKeyRule;
    }
}
