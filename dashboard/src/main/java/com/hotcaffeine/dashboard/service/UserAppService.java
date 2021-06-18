package com.hotcaffeine.dashboard.service;

import java.util.Set;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.model.App;
import com.hotcaffeine.data.store.IRedis;

/**
 * @Author yongweizhao
 * @Date 2021/2/1 18:19
 */
@Service
public class UserAppService {
    public static final String USER_APP_KEY = "ua";

    @Resource
    private IRedis redis;

    @Autowired
    private AppService appService;

    public void insert(String userName, String app) {
        redis.sadd(buildKey(userName), app);
    }

    public Result<?> saveUserApp(App app, String userName) {
        appService.insert(app);
        insert(userName, app.getAppName());
        return Result.success();
    }

    public Set<String> selectUserApp(String userName) {
        Set<String> appSet = redis.smembers(buildKey(userName));
        if (appSet == null) {
            return null;
        }
        return appSet;
    }

    public void del(String userName) {
        redis.del(buildKey(userName));
    }
    
    private String buildKey(String userName) {
        return USER_APP_KEY + userName;
    }
}
