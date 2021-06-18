package com.hotcaffeine.client.etcd;

import org.junit.Test;

import com.hotcaffeine.common.etcd.DefaultEtcdConfig;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.common.util.EventBusUtil;

public class AppEtcdClientTest {

    @Test
    public void test() throws InterruptedException {
        
        String appName = "core.api-web-mobile.hotkey";
        KeyRuleCacher keyRuleCacher = new KeyRuleCacher(appName);
        EventBusUtil.register(keyRuleCacher);
        
        DefaultEtcdConfig defaultEtcdConfig = new DefaultEtcdConfig();
        defaultEtcdConfig.init(appName);
        AppEtcdClient appEtcdClient = new AppEtcdClient(defaultEtcdConfig);
        appEtcdClient.start();
        Thread.sleep(10 * 60 * 1000);
    }

}
