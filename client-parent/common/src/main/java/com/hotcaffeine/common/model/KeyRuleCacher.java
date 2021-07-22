package com.hotcaffeine.common.model;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.common.cache.LocalCache;
import com.hotcaffeine.common.util.ClientLogger;

import io.etcd.jetcd.shaded.com.google.common.eventbus.Subscribe;

/**
 * 规则和缓存
 * 
 * @author yongfeigao
 * @date 2021年1月19日
 */
public class KeyRuleCacher implements KeyRuleCacherMBean {
    // keyRule map
    private ConcurrentMap<String, KeyRule> keyRuleMap = new ConcurrentHashMap<>();

    // 前缀匹配的 keyRule 列表
    private volatile List<KeyRule> prefixKeyRuleList = new ArrayList<>(0);

    // cache map
    private ConcurrentMap<String, CacheRule> cacheRuleMap = new ConcurrentHashMap<>();

    public static final String MBEAN_NAME = "com.hotcaffeine:name=ruleCache";

    private String appName;

    public KeyRuleCacher(String appName, boolean registerMBean) {
        this.appName = appName;
        if (registerMBean) {
            registerMBean();
        }
    }

    public KeyRuleCacher(String appName) {
        this(appName, true);
    }

    private void registerMBean() {
        try {
            ObjectName objectName = new ObjectName(MBEAN_NAME);
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ClientLogger.getLogger().info("appName:{} register mbean:{}", appName, MBEAN_NAME);
            mBeanServer.registerMBean(this, objectName);
        } catch (Throwable e) {
            ClientLogger.getLogger().warn("mqmetrics mbean register error:{}", e.getMessage());
        }
    }

    /**
     * 查找rule
     * 
     * @param key
     * @return KeyRule
     */
    public KeyRule findRule(String key) {
        if (KeyRule.isFullRule(key)) {
            String[] keyArray = key.split(KeyRule.SPLITER);
            return getRule(keyArray[0]);
        } else {
            return findPrefixRule(key);
        }
    }

    /**
     * 获取rule
     * 
     * @param key
     * @return KeyRule
     */
    public KeyRule getRule(String key) {
        KeyRule keyRule = keyRuleMap.get(key);
        if (keyRule == null) {
            keyRule = keyRuleMap.get(KeyRule.DEFAULT_KEY);
        }
        return keyRule;
    }
    
    /**
     * 是否是精准规则
     * @param ruleKey
     * @return
     */
    public boolean isExactRule(String ruleKey) {
        return keyRuleMap.get(ruleKey) != null;
    }

    /**
     * 前缀匹配
     * 
     * @param key
     * @return
     */
    public KeyRule findPrefixRule(String key) {
        List<KeyRule> keyRuleList = prefixKeyRuleList;
        for (KeyRule keyRule : keyRuleList) {
            if (key.startsWith(keyRule.getKey())) {
                return keyRule;
            }
        }
        return getRule(KeyRule.DEFAULT_KEY);
    }

    public CacheRule getCacheRule(KeyRule keyRule) {
        CacheRule cacheRule = cacheRuleMap.get(keyRule.getCacheName());
        if (cacheRule == null) {
            return null;
        }
        return cacheRule;
    }

    public LocalCache<ValueModel> getCache(KeyRule keyRule) {
        CacheRule cacheRule = getCacheRule(keyRule);
        if (cacheRule == null) {
            return null;
        }
        return cacheRule.getCache();
    }

    @Override
    public String getValue(String key) {
        KeyRule keyRule = findRule(key);
        if (keyRule == null) {
            return null;
        }
        LocalCache<ValueModel> localCache = getCache(keyRule);
        if (localCache == null) {
            return null;
        }
        return JSON.toJSONString(localCache.get(keyRule.stripRuleKey(key)));
    }
    
    public KeyRule findKeyRuleByKey(String key) {
        for (KeyRule keyRule : prefixKeyRuleList) {
            if (keyRule.getKey().equals(key)) {
                return keyRule;
            }
        }
        return getRule(key);
    }

    @Subscribe
    public void ruleChange(KeyRuleChangeEvent event) {
        ClientLogger.getLogger().info("appName:{} receive keyRule:{}", appName, event.getKeyRules());
        List<KeyRule> keyRuleList = event.getKeyRules();
        if (keyRuleList == null) {
            return;
        }
        updateKeyRule(keyRuleList);
    }

    /**
     * 更新KeyRule
     * 
     * @param keyRuleList
     */
    public void updateKeyRule(List<KeyRule> keyRuleList) {
        if (keyRuleList == null) {
            keyRuleList = new ArrayList<>(0);
        }
        // 移除禁用的keyRule
        removeDisabled(keyRuleList);
        // 更新前缀规则
        resetPrefixKeyRule(keyRuleList);
        // 移除不存在的
        removeNotExist(keyRuleMap, keyRuleList, (ruleKey, keyRule) -> {
            return ruleKey.equals(keyRule.getKey());
        });
        // 更新存在的
        for (KeyRule keyRule : keyRuleList) {
            KeyRule oldKeyRule = keyRuleMap.get(keyRule.getKey());
            if (keyRule.equals(oldKeyRule)) {
                continue;
            }
            keyRule.initDestQps();
            keyRuleMap.put(keyRule.getKey(), keyRule);
            ClientLogger.getLogger().info("appName:{} keyRule update from {} to {}", appName, oldKeyRule, keyRule);
        }
    }
    
    private void removeDisabled(List<KeyRule> keyRuleList) {
        Iterator<KeyRule> iterator = keyRuleList.iterator();
        while (iterator.hasNext()) {
            KeyRule keyRule = iterator.next();
            if (keyRule.isDisabled()) {
                iterator.remove();
                ClientLogger.getLogger().info("remove disabled keyRule:{}", keyRule);
            }
        }
    }

    /**
     * 重置keyRule
     * 
     * @param keyRuleList
     */
    private void resetPrefixKeyRule(List<KeyRule> keyRuleList) {
        List<KeyRule> newPrefixKeyRuleList = filterPrefixKeyRuleList(keyRuleList);
        // 没变不修改
        if (!changed(newPrefixKeyRuleList)) {
            return;
        }
        ClientLogger.getLogger().info("appName:{} prefixKeyRuleList update from {} to {}", appName,
                this.prefixKeyRuleList.stream().map(kr -> kr.getKey()).collect(Collectors.toList()),
                newPrefixKeyRuleList.stream().map(kr -> kr.getKey()).collect(Collectors.toList()));
        this.prefixKeyRuleList = newPrefixKeyRuleList;
    }

    /**
     * 判断是否发生变更
     * 
     * @param newPrefixKeyRuleList
     * @return
     */
    private boolean changed(List<KeyRule> newPrefixKeyRuleList) {
        if (prefixKeyRuleList.size() != newPrefixKeyRuleList.size()) {
            return true;
        }
        for (int i = 0; i < prefixKeyRuleList.size(); ++i) {
            KeyRule oldKeyRule = prefixKeyRuleList.get(i);
            KeyRule newKeyRule = newPrefixKeyRuleList.get(i);
            if (!oldKeyRule.equals(newKeyRule)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤出前缀匹配规则
     * 
     * @param keyRuleList
     * @return
     */
    private List<KeyRule> filterPrefixKeyRuleList(List<KeyRule> keyRuleList) {
        // 更新前缀规则
        List<KeyRule> newPrefixKeyRuleList = new ArrayList<>();
        Iterator<KeyRule> keyRuleIterator = keyRuleList.iterator();
        while (keyRuleIterator.hasNext()) {
            KeyRule keyRule = keyRuleIterator.next();
            if (keyRule.isPrefix()) {
                keyRule.initDestQps();
                newPrefixKeyRuleList.add(keyRule);
                keyRuleIterator.remove();
            }
        }
        // 倒序排序
        Collections.sort(newPrefixKeyRuleList, (o1, o2) -> {
            return o2.getKey().compareTo(o1.getKey());
        });
        return newPrefixKeyRuleList;
    }

    @Subscribe
    public void cacheRuleChange(CacheRuleChangeEvent event) {
        List<CacheRule> cacheRuleList = event.getCacheRuleList();
        ClientLogger.getLogger().info("appName:{} receive cacheRule:{}", appName, cacheRuleList);
        updateCacheRule(cacheRuleList, true);
    }

    /**
     * 更新CacheRule
     * 
     * @param cacheRuleList
     */
    public void updateCacheRule(List<CacheRule> cacheRuleList, boolean initCache) {
        if (cacheRuleList == null) {
            return;
        }
        // 移除不存在的
        removeNotExist(cacheRuleMap, cacheRuleList, (cacheName, cacheRule) -> {
            return cacheName.equals(cacheRule.getName());
        });
        // 更新存在的
        for (CacheRule newCacheRule : cacheRuleList) {
            cacheRuleMap.computeIfAbsent(newCacheRule.getName(), k -> {
                if (initCache) {
                    newCacheRule.initCache();
                }
                return newCacheRule;
            }).update(newCacheRule, initCache);
        }
    }

    private <T, D> void removeNotExist(ConcurrentMap<String, T> map, List<D> dataList,
            BiPredicate<String, D> predicate) {
        for (String key : map.keySet()) {
            boolean needRemove = true;
            for (D data : dataList) {
                if (predicate.test(key, data)) {
                    needRemove = false;
                    break;
                }
            }
            if (needRemove) {
                T t = map.remove(key);
                ClientLogger.getLogger().warn("appName:{} remove:{}", appName, t);
            }
        }
    }

    public List<KeyRule> getPrefixKeyRuleList() {
        return prefixKeyRuleList;
    }

    public ConcurrentMap<String, KeyRule> getKeyRuleMap() {
        return keyRuleMap;
    }

    public List<KeyRule> getKeyRuleList() {
        List<KeyRule> keyRuleList = new ArrayList<>();
        keyRuleList.addAll(keyRuleMap.values());
        keyRuleList.addAll(prefixKeyRuleList);
        return keyRuleList;
    }
    
    /**
     * 任意选择一个KeyRule
     * 
     * @return
     */
    public KeyRule selectKeyRule() {
        for (KeyRule keyRule : keyRuleMap.values()) {
            return keyRule;
        }
        List<KeyRule> keyRuleList = prefixKeyRuleList;
        for (KeyRule keyRule : keyRuleList) {
            return keyRule;
        }
        return null;
    }
    
    public static class CacheRuleChangeEvent {
        private List<CacheRule> cacheRuleList;

        public CacheRuleChangeEvent(List<CacheRule> cacheRuleList) {
            this.cacheRuleList = cacheRuleList;
        }

        public List<CacheRule> getCacheRuleList() {
            return cacheRuleList;
        }
    }
    
    
    public static class KeyRuleChangeEvent {
        private List<KeyRule> keyRules;

        public KeyRuleChangeEvent(List<KeyRule> keyRules) {
            this.keyRules = keyRules;
        }

        public List<KeyRule> getKeyRules() {
            return keyRules;
        }

        public void setKeyRules(List<KeyRule> keyRules) {
            this.keyRules = keyRules;
        }
    }
}
