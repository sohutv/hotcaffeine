package com.hotcaffeine.worker.cache;

import java.util.List;

import org.junit.Test;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.etcd.DefaultEtcdConfig;
import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.ServiceLoaderUtil;


public class AppKeyRuleCacherTest {

    @Test
    public void test() throws InterruptedException {
        String user = "worker";
        String password = "AKJ@(*#@dya23";
        
        IEtcdConfig etcdConfig = ServiceLoaderUtil.loadService(IEtcdConfig.class, DefaultEtcdConfig.class);
        etcdConfig.init(user);
        etcdConfig.setPassword(password);
        EtcdClient etcdClient = new EtcdClient(etcdConfig.getEndpoints(), etcdConfig.getUser(), etcdConfig.getPassword());
        
        AppKeyRuleCacher appKeyRuleCacher = new AppKeyRuleCacher();
        
        List<KV> kvList2 = etcdClient.getPrefix(etcdConfig.getRulePath());
        kvList2.forEach(kv -> {
            String appName = kv.getKey().replace(etcdConfig.getRulePath(), "");
            if (StringUtils.isEmpty(appName)) {
                return;
            }
            String ruleJson = kv.getValue();
            List<KeyRule> keyRules = JsonUtil.toList(ruleJson, KeyRule.class);
            appKeyRuleCacher.update(appName, keyRules);
        });
        
        
        etcdClient.watch(etcdConfig.getRulePath(), kv -> {
            String appName = kv.getKey().replace(etcdConfig.getRulePath(), "");
            if (StringUtils.isEmpty(appName)) {
                return;
            }
            String ruleJson = kv.getValue();
            List<KeyRule> keyRules = JsonUtil.toList(ruleJson, KeyRule.class);
            appKeyRuleCacher.update(appName, keyRules);
        });
        
        
        
        Thread.sleep(10 * 60 * 1000);
    }

}
