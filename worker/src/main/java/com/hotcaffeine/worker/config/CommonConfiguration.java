package com.hotcaffeine.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotcaffeine.common.etcd.DefaultEtcdConfig;
import com.hotcaffeine.common.etcd.IEtcdConfig;
import com.hotcaffeine.common.model.ConsistentHashServer.HashFunction;
import com.hotcaffeine.common.model.ConsistentHashServer.Murmur3HashFunction;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.MemoryMQ;
import com.hotcaffeine.common.util.MemoryMQGroup;
import com.hotcaffeine.common.util.MemoryMQGroup.IGroupBy;
import com.hotcaffeine.common.util.ServiceLoaderUtil;
import com.hotcaffeine.worker.consumer.DashboardConsumer;
import com.hotcaffeine.worker.consumer.NewKeyConsumer;

/**
 * 通用配置
 * 
 * @Description:
 * @author yongfeigao
 * @date 2018年6月13日
 */
@Configuration
public class CommonConfiguration {

    @Value("${thread.count}")
    private int threadCount;

    @Bean
    @ConfigurationProperties(prefix = "etcd")
    public IEtcdConfig etcdConfig() {
        IEtcdConfig etcdConfig = ServiceLoaderUtil.loadService(IEtcdConfig.class, DefaultEtcdConfig.class);
        etcdConfig.init(null);
        return etcdConfig;
    }

    /**
     * 新key内存队列
     * 
     * @param newKeyConsumer
     * @return
     */
    @Bean
    public MemoryMQGroup<KeyCount> newKeyMemoryMQ(NewKeyConsumer newKeyConsumer) {
        HashFunction hashFunction = new Murmur3HashFunction();
        // 构建分组依据
        IGroupBy<KeyCount> groupBy = new IGroupBy<KeyCount>() {
            public long groupValue(KeyCount keyCount) {
                return hashFunction.hash(keyCount.getKey()) & Long.MAX_VALUE;
            }
        };
        if (threadCount == 0) {
            threadCount = Runtime.getRuntime().availableProcessors();
        }
        // 构建内存mq分组
        return new MemoryMQGroup<KeyCount>(groupBy, threadCount, 100000, "newKeyMQ",
                newKeyConsumer);
    }

    /**
     * dashboard内存队列
     * 
     * @param dashboardConsumer
     * @return
     */
    @Bean
    public MemoryMQ<KeyCount> dashboardMemoryMQ(DashboardConsumer dashboardConsumer) {
        MemoryMQ<KeyCount> memoryMQ = new MemoryMQ<>();
        memoryMQ.setConsumerName("dashboardMQ");
        // 最大缓存量
        memoryMQ.setBufferSize(100000);
        // 消费线程数
        memoryMQ.setConsumerThreadNum(2);
        // 以下三个条件限制了，100条或10秒内1条 就处理
        // 最小批量处理数量
        memoryMQ.setMinBatchDealSize(100);
        // 最小处理时间间隔和处理数量
        memoryMQ.setMinDealIntervalMillis(10000);
        memoryMQ.setMinDealIntervalBufferSize(1);

        memoryMQ.setMemoryMQConsumer(dashboardConsumer);
        memoryMQ.setDestroyOrder(40);
        memoryMQ.init();
        return memoryMQ;
    }
}
