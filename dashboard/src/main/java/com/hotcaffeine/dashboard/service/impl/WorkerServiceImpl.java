package com.hotcaffeine.dashboard.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.dashboard.common.domain.req.PageReq;
import com.hotcaffeine.dashboard.common.domain.req.SearchReq;
import com.hotcaffeine.dashboard.model.Worker;
import com.hotcaffeine.dashboard.service.WorkerService;

/**
 * @Author: liyunfeng31
 * @Date: 2020/4/17 18:19
 */
@Service
public class WorkerServiceImpl implements WorkerService {

    @Resource
    private EtcdClient etcdClient;
    
    @Autowired
    private IEtcdConfig etcdConfig;
    
    @Override
    public List<Worker> pageWorker(PageReq page, SearchReq param) {
        List<KV> kvList = etcdClient.getPrefix(etcdConfig.getWorkerPath());
        List<Worker> workers = new ArrayList<>();
        for (KV kv : kvList) {
            String k = kv.getKey();
            String v = kv.getValue();
            Worker worker = new Worker(k, v);
            workers.add(worker);
        }
        return workers;
    }
}
