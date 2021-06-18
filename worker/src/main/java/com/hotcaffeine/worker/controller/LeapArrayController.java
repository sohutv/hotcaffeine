package com.hotcaffeine.worker.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.benmanes.caffeine.cache.Cache;
import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.data.metric.LeapArrayModel;
import com.hotcaffeine.data.metric.LeapArrayModel.WindowWrapModel;
import com.hotcaffeine.data.util.Result;
import com.hotcaffeine.data.util.Status;
import com.hotcaffeine.worker.cache.AppCaffeineCache;
import com.hotcaffeine.worker.metric.BucketLeapArray;
import com.hotcaffeine.worker.metric.WindowWrap;

import io.etcd.jetcd.shaded.com.google.common.hash.Hashing;

/**
 * 获取滑动窗口数据
 * 
 * @author yongfeigao
 * @date 2021年3月12日
 */
@RestController
@RequestMapping("leap")
public class LeapArrayController {
    
    @Autowired
    private AppCaffeineCache appCaffeineCache;
    
    /**
     * 滑动窗口
     * 
     * @param appName
     * @param ruleKey
     * @param key
     * @return
     */
    @RequestMapping("/window")
    public Result<?> window(String appName, String key) {
        if (StringUtils.isEmpty(appName) || StringUtils.isEmpty(key)) {
            return Result.getResult(Status.PARAM_ERROR);
        }
        CaffeineCache<BucketLeapArray> caffeineCache = appCaffeineCache.getCache(appName);
        if(caffeineCache == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        Cache<String, BucketLeapArray> cache = caffeineCache.getCache();
        if (cache == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        BucketLeapArray bucketLeapArray = cache.asMap().get(key);
        if (bucketLeapArray == null) {
            return Result.getResult(Status.NO_RESULT);
        }
        // 初始化数据
        LeapArrayModel leapArrayModel = new LeapArrayModel();
        leapArrayModel.setIntervalInMs(bucketLeapArray.getIntervalInMs());
        leapArrayModel.setSampleCount(bucketLeapArray.getSampleCount());
        leapArrayModel.setWindowLengthInMs(bucketLeapArray.getWindowLengthInMs());
        leapArrayModel.setLiveTime(bucketLeapArray.liveTime());
        leapArrayModel.setSurvivalTime(bucketLeapArray.survivalTime());
        leapArrayModel.setTotalCount(bucketLeapArray.getTotalCount());

        // 获取窗口
        AtomicReferenceArray<WindowWrap<LongAdder>> windowArray = bucketLeapArray.getArray();
        int size = windowArray.length();
        List<WindowWrapModel> windowWrapModelList = new ArrayList<>(size);

        long curTime = System.currentTimeMillis();
        for (int i = 0; i < size; i++) {
            WindowWrap<LongAdder> windowWrap = windowArray.get(i);
            if (windowWrap == null) {
                continue;
            }
            WindowWrapModel windowWrapModel = new WindowWrapModel();
            windowWrapModel.setWindowStart(windowWrap.windowStart());
            windowWrapModel.setValue(windowWrap.value().longValue());
            windowWrapModel.setDeprecated(bucketLeapArray.isWindowDeprecated(curTime, windowWrap));
            windowWrapModel.setCounting(windowWrap.isTimeInWindow(curTime));
            windowWrapModelList.add(windowWrapModel);
        }
        leapArrayModel.setWindowList(windowWrapModelList);
        return Result.getResult(leapArrayModel);
    }
    
    
    
    @RequestMapping("/hash")
    public String hash(String key) {
        return String.valueOf(Hashing.murmur3_128().hashBytes(key.getBytes()).asLong());
    }
}
