package com.hotcaffeine.dashboard.service;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.ConsistentHashServer;

/**
 * worker服务
 * 
 * @author yongfeigao
 * @date 2021年6月8日
 */
@Component
public class WorkerServer {
    @Resource
    private EtcdClient etcdClient;

    @Autowired
    private IEtcdConfig etcdConfig;

    private volatile ConsistentHashServer consistentHashServer = new ConsistentHashServer();
    
    @Scheduled(fixedRate = 3000)
    public void updateWorker() {
        List<KV> kvList = etcdClient.getPrefix(etcdConfig.getWorkerPath());
        if (kvList == null || kvList.size() == 0) {
            consistentHashServer.clear();
            return;
        }
        ConsistentHashServer tmpConsistentHashServer = new ConsistentHashServer();
        for (KV kv : kvList) {
            tmpConsistentHashServer.add(kv.getValue());
        }
        consistentHashServer = tmpConsistentHashServer;
    }
    
    public String getServer() {
        return getServer("");
    }
    
    public String getServer(String key) {
        return consistentHashServer.select(key);
    }
}
