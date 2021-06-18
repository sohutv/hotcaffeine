package com.hotcaffeine.worker.consumer;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.MemoryMQ.BatchMemoryMQConsumer;
import com.hotcaffeine.worker.netty.dashboard.NettyClient;

/**
 * 热key推送到dashboard
 * 
 * @author yongfeigao
 * @date 2021年5月8日
 */
@Component
public class DashboardConsumer implements BatchMemoryMQConsumer<KeyCount> {

    @Override
    public void consume(List<KeyCount> keyCountList) throws Exception {
        NettyClient.getInstance().flushToDashboard(keyCountList);
    }
}
