package com.hotcaffeine.dashboard.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.model.Rule;
import com.hotcaffeine.dashboard.model.Rules;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;
import com.hotcaffeine.dashboard.service.RuleService;

/**
 * @Author: liyunfeng31
 * @Date: 2020/4/17 18:18
 */
@Service
public class RuleServiceImpl implements RuleService {
    // 这里先直接写死,避免fastjson生成方式导致的展示顺序混乱
    public static final String KEY_RULE_JSON_DEFAULT = "[{\"key\":\"*\",\"prefix\":false,\"cacheName\":\"default\",\"interval\":1,\"threshold\":100,\"enableLocalDetector\":false,\"topkCount\":100,\"useTopKAsHotKey\":false}]";
    
    public static final String CACHE_KEY_JSON_DEFAULT = "[{\"name\":\"default\",\"duration\":200,\"size\":10000}]";

    @Resource
    private EtcdClient etcdClient;
    
    @Autowired
    private IEtcdConfig etcdConfig;
    
    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @Override
    public Rules selectRules(String app) {
        String v = etcdClient.get(etcdConfig.getRulePath() + app);
        if (v == null) {
            return new Rules();
        }
        return new Rules(app, v);
    }

    @Override
    public int updateRule(Rules rules) {
        String app = rules.getApp();
        etcdClient.put(etcdConfig.getRulePath() + app, rules.getRules());
        return 1;
    }

    @Override
    public Integer add(Rules rules) {
        String app = rules.getApp();
        etcdClient.put(etcdConfig.getRulePath() + app, rules.getRules());
        return 1;
    }


    @Override
    public int delRule(String app, String updater) {
        etcdClient.delete(etcdConfig.getRulePath() + app);
        return 1;
    }

    @Override
    public List<Rules> pageKeyRule(PageReq page, String appName) {
        List<KV> kvList = etcdClient.getPrefix(etcdConfig.getRulePath());
        List<Rules> rules = new ArrayList<>();
        for (KV kv : kvList) {
            String v = kv.getValue();
            if (StringUtils.isEmpty(v)) {
                continue;
            }
            String key = kv.getKey();
            String k = key.replace(etcdConfig.getRulePath(), "");
            if (k.equals(appName)) {
                rules.add(new Rules(k, v));
            }
        }
        return rules;
    }

    @Override
    public int save(Rules rules) {
        String app = rules.getApp();
        etcdClient.put(etcdConfig.getRulePath() + app, rules.getRules());
        return 1;
    }

    @Override
    public List<String> listRules(String app) {
        List<KV> kvList = etcdClient.getPrefix(etcdConfig.getRulePath());
        List<String> rules = new ArrayList<>();
        for (KV kv : kvList) {
            String v = kv.getValue();
            if (StringUtils.isEmpty(v)) {
                continue;
            }
            String key = kv.getKey();
            String appKey = key.replace(etcdConfig.getRulePath(), "");
            List<Rule> rs = JSON.parseArray(v, Rule.class);
            for (Rule r : rs) {
                rules.add(appKey + "-" + r.getKey());
            }
        }
        return rules;
    }

    @Override
    public int initDefaultRules(String appName, String userName) {
        // 初始化缓存规则
        etcdClient.put(etcdConfig.getCachePath() + appName, CACHE_KEY_JSON_DEFAULT);
        
        Rules rules = new Rules();
        rules.setApp(appName);
        rules.setUpdateUser(userName);
        rules.setUpdateTime(new Date());
        rules.setRules(KEY_RULE_JSON_DEFAULT);
        return save(rules);
    }

    @Override
    public List<KeyRule> keyRules(String appName) {
        KeyRuleCacher keyRuleCacher = appKeyRuleCacher.getKeyRuleCacher(appName);
        if (keyRuleCacher == null) {
            return null;
        }
        return keyRuleCacher.getKeyRuleList();
    }
}
