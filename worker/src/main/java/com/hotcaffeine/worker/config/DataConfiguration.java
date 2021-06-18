package com.hotcaffeine.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotcaffeine.common.util.ServiceLoaderUtil;
import com.hotcaffeine.data.CounterDistributionStore;
import com.hotcaffeine.data.TopHotKeyStore;
import com.hotcaffeine.data.store.IRedis;
import com.hotcaffeine.data.store.PooledRedis;
import com.hotcaffeine.data.store.RedisConfiguration;

@Configuration
public class DataConfiguration {

    @Bean
    @ConfigurationProperties("redis")
    public RedisConfiguration redisConfiguration() {
        return new RedisConfiguration();
    }

    @Bean
    public IRedis redis(RedisConfiguration redisConfiguration) throws Exception {
        IRedis redis = ServiceLoaderUtil.loadService(IRedis.class, PooledRedis.class);
        redis.init(redisConfiguration);
        return redis;
    }

    @Bean
    public CounterDistributionStore counterDistributionStore(IRedis redis) {
        CounterDistributionStore counterDistributionStore = new CounterDistributionStore();
        counterDistributionStore.setRedis(redis);
        return counterDistributionStore;
    }

    @Bean
    public TopHotKeyStore topHotKeyStore(IRedis redis) {
        TopHotKeyStore topHotKeyStore = new TopHotKeyStore();
        topHotKeyStore.setRedis(redis);
        return topHotKeyStore;
    }
}