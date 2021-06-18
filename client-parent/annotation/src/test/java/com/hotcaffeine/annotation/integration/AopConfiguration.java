package com.hotcaffeine.annotation.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.hotcaffeine.annotation.HotCaffeineAspect;
import com.hotcaffeine.client.HotCaffeine;
import com.hotcaffeine.client.HotCaffeineDetector;
import com.hotcaffeine.common.etcd.DefaultEtcdConfig;

@Configuration
@ComponentScan("com.hotcaffeine.annotation.integration")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfiguration {
    
    @Autowired
    private RedisCache redisCache;

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public HotCaffeineDetector hotCaffeineDetector() {
        DefaultEtcdConfig defaultEtcdConfig = new DefaultEtcdConfig();
        defaultEtcdConfig.init("api");
        return new HotCaffeineDetector(defaultEtcdConfig);
    }

    @Bean
    public HotCaffeine hotCaffeine() {
        return hotCaffeineDetector().build(redisCache);
    }

    @Bean
    public HotCaffeineAspect hotCaffeineAspect() {
        return new HotCaffeineAspect();
    }
}
