package com.hotcaffeine.dashboard.etcd;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.CacheRule;
import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.util.IpUtil;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.dashboard.rule.AppKeyRuleCacher;

/**
 * dashboard etcd
 * 
 * @author yongfeigao
 * @date 2021年6月3日
 */
@Component
public class DashboardEtcdClient implements Destroyable {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IEtcdConfig etcdConfig;

    @Resource
    private EtcdClient etcdClient;

    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @Value("${dashbord.port:11112}")
    private int dashboardPort;

    private ScheduledExecutorService scheduledExecutorService;

    @PostConstruct
    public void start() {
        // 拉取keyCache
        fetchCacheRule();
        // 拉取keyRule
        fetchKeyRule();
        // 监听keyCache
        watchKeyCache();
        // 监听keyRule
        watchKeyRule();
        // 保活
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                uploadSelfInfo();
            } catch (Exception e) {
                logger.error("uploadSelfInfo error", e);
            }

        }, 3, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 获取keyRule列表
     * @param appName
     * @return
     */
    public List<KeyRule> getKeyRuleList(String appName) {
        String keyRuleString = etcdClient.get(etcdConfig.getRulePath() + appName);
        if(keyRuleString == null) {
            return null;
        }
        return JsonUtil.toList(keyRuleString, KeyRule.class);
    }

    /**
     * 监听keyCache
     */
    private void watchKeyCache() {
        etcdClient.watch(etcdConfig.getCachePath(), kv -> fetchCacheRule());
    }

    /**
     * 监听keyRule
     */
    public <T> void watchKeyRule() {
        etcdClient.watch(etcdConfig.getRulePath(), kv -> fetchKeyRule());
    }

    /**
     * 抓取keyRule
     */
    private void fetchKeyRule() {
        fetchConfig(etcdConfig.getRulePath(), (appName, keyRuleString) -> {
            appKeyRuleCacher.update(appName, JsonUtil.toList(keyRuleString, KeyRule.class));
        });
    }

    /**
     * 抓取keyCache
     */
    private void fetchCacheRule() {
        fetchConfig(etcdConfig.getCachePath(), (appName, keyRuleString) -> {
            appKeyRuleCacher.updateKeyCache(appName, JsonUtil.toList(keyRuleString, CacheRule.class));
        });
    }

    /**
     * 抓取配置
     * 
     * @param path
     * @param biConsumer
     */
    private void fetchConfig(String path, BiConsumer<String, String> biConsumer) {
        List<KV> kvList = etcdClient.getPrefix(path);
        if (kvList == null || kvList.size() == 0) {
            return;
        }
        kvList.forEach(kv -> {
            try {
                logger.info("fetchConfig path:{} kv:{}", path, kv);
                String appName = kv.getKey().replace(path, "");
                if (StringUtils.isEmpty(appName)) {
                    etcdClient.delete(kv.getKey());
                    return;
                }
                biConsumer.accept(appName, kv.getValue());
            } catch (Exception e) {
                logger.error("parse kv:{} failure:{}", kv, e.getMessage());
            }
        });
    }

    private void uploadSelfInfo() {
        etcdClient.put(buildKey(), buildValue(), 32);
    }

    private void deleteSelfInfo() {
        etcdClient.delete(buildKey());
    }

    private String buildKey() {
        String hostName = IpUtil.getHostName();
        return etcdConfig.getDashboardPath() + hostName;
    }

    private String buildValue() {
        String ip = IpUtil.getIp();
        return ip + ":" + dashboardPort;
    }

    @Override
    public void destroy() throws Exception {
        scheduledExecutorService.shutdown();
        deleteSelfInfo();
        etcdClient.close();
    }

    @Override
    public int order() {
        return 10;
    }
}
