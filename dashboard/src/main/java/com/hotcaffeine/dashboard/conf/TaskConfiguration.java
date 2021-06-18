package com.hotcaffeine.dashboard.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hotcaffeine.dashboard.task.CleanCacheTask;
import com.hotcaffeine.data.store.IRedis;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.jedis.JedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

/**
 * @Author yongweizhao
 * @Date 2021/3/8 17:14
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "1m")
public class TaskConfiguration {

    @Bean
    public CleanCacheTask cleanCacheTask() {
        CleanCacheTask cleanCacheTask = new CleanCacheTask();
        return cleanCacheTask;
    }

    @Bean
    public LockProvider lockProvider(IRedis redis) {
        if (redis.getPool() != null) {
            return new JedisLockProvider(redis.getPool());
        } else {
            return new JedisLockProvider(redis.getJedisCluster(), "LC");
        }
    }
}
