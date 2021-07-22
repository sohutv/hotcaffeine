package com.hotcaffeine.worker.pusher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.MemoryMQ;

/**
 * 将热key推送到dashboard供入库
 * @author wuweifeng
 * @version 1.0
 * @date 2020-08-31
 */
@Component
public class DashboardPusher implements IPusher {
    
    @Autowired
    private MemoryMQ<KeyCount> dashboardMemoryMQ;
    
    @Override
    public void push(KeyCount keyCount) {
        if (keyCount.isInner()) {
            return;
        }
        dashboardMemoryMQ.offer(keyCount);
    }
}
