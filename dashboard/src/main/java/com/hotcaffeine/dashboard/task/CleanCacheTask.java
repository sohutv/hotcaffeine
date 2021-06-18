package com.hotcaffeine.dashboard.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import com.hotcaffeine.dashboard.service.HotKeyService;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

/**
 * @Author yongweizhao
 * @Date 2021/3/9 10:22
 */
public class CleanCacheTask {


    @Autowired
    private HotKeyService hotKeyService;
    
    @Scheduled(cron = "*/10 * * * * *")
    @SchedulerLock(name = "cleanUpRedisCache", lockAtMostFor = "8s", lockAtLeastFor = "5s")
    public void cleanUpRedisCache() {
        try {
            hotKeyService.cleanUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //logger.info("clean up redis data cost:{} ms", System.currentTimeMillis() - start);
    }
}
