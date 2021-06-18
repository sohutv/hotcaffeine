package com.hotcaffeine.worker.etcd;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.etcd.EtcdClient;
import com.hotcaffeine.common.etcd.EtcdClient.KV;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.util.IpUtil;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.worker.cache.AppKeyRuleCacher;
import com.hotcaffeine.worker.netty.dashboard.NettyClient;

/**
 * worker端对etcd相关的处理
 *
 * @author wuweifeng wrote on 2019-12-10
 * @version 1.0
 */
@Component
public class WorkerEtcdClient implements Destroyable {
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private IEtcdConfig etcdConfig;

    @Value("${netty.port}")
    private int port;

    @Value("${local.address}")
    private String localAddress;

    /**
     * 是否可以继续上报自己的ip
     */
    private volatile boolean canUpload = true;
    
    private EtcdClient etcdClient;
    
    @Autowired
    private AppKeyRuleCacher appKeyRuleCacher;

    @PostConstruct
    public void start() {
        // 初始化etcd
        this.etcdClient = new EtcdClient(etcdConfig.getEndpoints(), etcdConfig.getUser(), etcdConfig.getPassword());
        // 监听rule
        watchRule();
        // 拉取规则
        pullRules();
        // 更新dashboard
        updateDashboardIp();
        // 注册自己
        makeSureSelfOn();
    }

    /**
     * 启动回调监听器，监听rule变化
     */
    public void watchRule() {
        String watchPath = etcdConfig.isDefaultWorker() ? etcdConfig.getRulePath() : etcdConfig.getWorkerForAppPath();
        etcdClient.watch(watchPath, kv -> {
            pullRules();
        });
    }

    public void pullRules() {
        if(etcdConfig.isDefaultWorker()) {
            List<KV> kvList = etcdClient.getPrefix(etcdConfig.getRulePath());
            if(kvList == null) {
                return;
            }
            kvList.forEach(kv -> {
                ruleChange(kv);
            });
        } else {
            String value = etcdClient.get(etcdConfig.getWorkerForAppPath());
            if (!StringUtils.isEmpty(value)) {
                List<KeyRule> keyRules = JsonUtil.toList(value, KeyRule.class);
                appKeyRuleCacher.update(etcdConfig.getWorkerForApp(), keyRules);
            }
        }
    }

    /**
     * 每隔30秒去获取一下dashboard的地址
     */
    @Scheduled(fixedRate = 3000)
    public void fetchDashboardIp() {
        updateDashboardIp();
    }
    
    private void updateDashboardIp() {
        try {
            //获取DashboardIp
            List<KV> kvList = etcdClient.getPrefix(etcdConfig.getDashboardPath());
            //是空，给个警告
            if (kvList == null || kvList.size() == 0) {
                logger.warn("very important warn !!! Dashboard ip is null!!!");
                return;
            }
            String dashboardIp = kvList.get(0).getValue();
            NettyClient.getInstance().connect(dashboardIp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * rule发生变化时，更新缓存的rule
     */
    private synchronized void ruleChange(KV kv) {
        String appName = kv.getKey().replace(etcdConfig.getRulePath(), "");
        if (StringUtils.isEmpty(appName)) {
            return;
        }
        String ruleJson = kv.getValue();
        List<KeyRule> keyRules = JsonUtil.toList(ruleJson, KeyRule.class);
        appKeyRuleCacher.update(appName, keyRules);
    }

    public void removeNodeInfo() {
        try {
            etcdClient.delete(buildKey());
        } catch (Exception e) {
            logger.error("worker connect to etcd failure");
        }
    }

    /**
     * 每隔一会去check一下，自己还在不在etcd里
     */
    public void makeSureSelfOn() {
        //开启上传worker信息
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> {

            try {
                if (canUpload) {
                    uploadSelfInfo();
                }
            } catch (Exception e) {
                //do nothing
            }

        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * 通过http请求手工上传信息到etcd，适用于正常使用过程中，etcd挂掉，导致worker租期到期被删除，无法自动注册
     */
    private void uploadSelfInfo() {
        etcdClient.put(buildKey(), buildValue(), 8);
    }
    
    public void setTopkTaskTime(String time) {
        etcdClient.put(buildTopkTaskKey(), time, 60);
    }
    
    private void removeTopkTaskTime() {
        etcdClient.delete(buildTopkTaskKey());
    }
    
    private String buildTopkTaskKey() {
        return etcdConfig.getTopkPath() + getIp();
    }
    
    /**
     * 检测topk是否可以merge
     * @param time
     * @return
     */
    public boolean topkMergeTaskTimeOK(String time) {
        List<KV> kvList = etcdClient.getPrefix(etcdConfig.getTopkPath());
        if (kvList == null || kvList.size() == 0) {
            return false;
        }
        for (KV kv : kvList) {
            if (kv.getValue().compareTo(time) < 0) {
                return false;
            }
        }
        return true;
    }
    
    private String buildKey() {
        String workerPath = etcdConfig.getDefaultWorkerPath();
        if (!etcdConfig.isDefaultWorker()) {
            workerPath = etcdConfig.getWorkerForAppPath();
        }
        return workerPath + "/" + IpUtil.getHostName();
    }
    
    private String getIp() {
        if (!StringUtils.isEmpty(localAddress)) {
            return localAddress;
        }
        return IpUtil.getIp();
    }

    private String buildValue() {
        return getIp() + ":" + port;
    }

    @Override
    public void destroy() throws Exception {
        this.canUpload = false;
        removeTopkTaskTime();
        removeNodeInfo();
        this.etcdClient.close();
    }

    @Override
    public int order() {
        return 10;
    }

}