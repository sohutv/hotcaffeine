package com.hotcaffeine.dashboard.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.dashboard.common.domain.Result;
import com.hotcaffeine.dashboard.model.App;
import com.hotcaffeine.data.store.IRedis;

/**
 * @Author yongweizhao
 * @Date 2021/2/1 18:21
 */
@Service
public class AppService {

    public static final String APP_KEY = "app";

    @Autowired
    private IRedis redis;

    /**
     * 新建app
     * 
     * @param app
     */
    public void insert(App app) {
        redis.hset(APP_KEY, app.getAppName(), JSON.toJSONString(app));
    }

    /**
     * 根据appName查询
     * 
     * @param appName
     */
    public App queryByName(String appName) {
        String appString = redis.hget(APP_KEY, appName);
        if (appString == null) {
            return null;
        }
        return JSON.parseObject(appString, App.class);
    }

    /**
     * 查询所有的app
     */
    public Result<List<App>> queryAll() {
        Map<String, String> mapString = redis.hgetAll(APP_KEY);
        if (mapString == null) {
            return Result.getResult((Object) null);
        }
        List<App> apps = mapString.entrySet().stream().map(entry -> JSON.parseObject(entry.getValue(), App.class))
                .collect(Collectors.toList());
        return Result.getResult(apps);
    }

    /**
     * 查询所有的appName
     */
    public Set<String> queryAllAppName() {
        Map<String, String> mapString = redis.hgetAll(APP_KEY);
        if (mapString == null) {
            return null;
        }
        return mapString.keySet();
    }

    /**
     * 由用户手动创建的appName列表
     */
    public List<String> AppNameManuallyCreated() {
        Map<String, String> mapString = redis.hgetAll(APP_KEY);
        if (mapString == null) {
            return null;
        }
        List<String> apps = mapString.entrySet().stream().map(entry -> JSON.parseObject(entry.getValue(), App.class))
                .filter(app -> app.getSource() == 2).map(app -> app.getAppName()).collect(Collectors.toList());
        return apps;
    }

}
