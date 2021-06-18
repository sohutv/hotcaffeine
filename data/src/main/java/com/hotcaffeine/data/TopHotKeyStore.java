package com.hotcaffeine.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.data.metric.HotKey;
import com.hotcaffeine.data.metric.TopHotKey;
import com.hotcaffeine.data.store.IRedis;

import redis.clients.jedis.params.SetParams;

/**
 * top hot key
 * 
 * @author yongfeigao
 * @date 2021年3月4日
 */
public class TopHotKeyStore {

    private IRedis redis;

    /**
     * 存储worker数据
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @param topHotKey
     */
    public void storeWorkerData(String appName, String ruleKey, long time, TopHotKey topHotKey) {
        // 存储topk统计
        String key = buildWorkerKey(appName, ruleKey, new SimpleDateFormat("yyyyMMddHHmm").format(new Date(time)));
        topHotKey.setHotKeyList(topHotKey.buildHotKeyList());
        redis.lpush(key, JSON.toJSONString(topHotKey));
        Long ttl = redis.ttl(key);
        if (ttl == -1) {
            // 5分钟后过期
            redis.expire(key, 5 * 60);
        }
    }

    public String getMergeTime() {
        return redis.get("tk:merge");
    }

    public String setMergeTime(String time) {
        return redis.set("tk:merge", time);
    }

    /**
     * 将各个worker数据合并
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @param size
     */
    public TopHotKey merge(String appName, String ruleKey, String timeKey, int size) {
        String lockKey = buildLockKey(appName, ruleKey, timeKey);
        String result = redis.set(lockKey, "", SetParams.setParams().nx().ex(60));
        if (!"OK".equals(result)) {
            return null;
        }
        String key = buildWorkerKey(appName, ruleKey, timeKey);
        List<String> list = redis.lrange(key, 0, -1);
        if (list == null || list.size() == 0) {
            return null;
        }
        TopHotKey topHotKey = new TopHotKey(size);
        list.forEach(thk -> {
            topHotKey.add(JSON.parseObject(thk, TopHotKey.class));
        });
        store(appName, ruleKey, timeKey, topHotKey);
        return topHotKey;
    }

    /**
     * 存储
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @param topHotKey
     */
    public void store(String appName, String ruleKey, String timeKey, TopHotKey topHotKey) {
        // 初始化调用量占用率
        List<HotKey> hotKeyList = topHotKey.buildHotKeyList();
        topHotKey.initCountRate(hotKeyList);
        // 设置时间，方便索引二级缓存
        String hourKey = timeKey.substring(0, timeKey.length() - 2);
        topHotKey.setMinute(timeKey.substring(timeKey.length() - 2));
        // 存储topk统计
        String key = buildKey(appName, ruleKey, hourKey);
        redis.lpush(key, JSON.toJSONString(topHotKey));
        Long ttl = redis.ttl(key);
        if (ttl == -1) {
            // 三天过期
            redis.expire(key, 3 * 24 * 60 * 60);
        }
        topHotKey.setHotKeyList(hotKeyList);
        // 存储hotkey 最多1000个
        String hotKeyKey = buildKey(appName, ruleKey, timeKey);
        int maxSize = hotKeyList.size() >= 1000 ? 1000 : hotKeyList.size();
        for (int i = 0; i < maxSize; ++i) {
            redis.rpush(hotKeyKey, JSON.toJSONString(hotKeyList.get(i)));
        }
        ttl = redis.ttl(hotKeyKey);
        if (ttl == -1) {
            // 三天过期
            redis.expire(hotKeyKey, 3 * 24 * 60 * 60);
        }
    }

    /**
     * 查询top
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @return
     */
    public List<TopHotKey> queryTopHotKey(String appName, String ruleKey, long time) {
        String key = buildKey(appName, ruleKey, time);
        List<String> list = redis.lrange(key, 0, -1);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.stream().map(s -> JSON.parseObject(s, TopHotKey.class)).collect(Collectors.toList());
    }

    /**
     * 查询热键
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @param start
     * @param end
     * @return
     */
    public List<HotKey> queryHotKey(String appName, String ruleKey, String time, int start, int end) {
        String key = buildKey(appName, ruleKey, time);
        List<String> list = redis.lrange(key, start, end);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.stream().map(s -> JSON.parseObject(s, HotKey.class)).map(h -> {
            h.setLiveTimeSecond(HotKey.toSecond(h.getLiveTime()));
            return h;
        }).collect(Collectors.toList());
    }

    /**
     * 查询热键数量
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @return
     */
    public Long queryHotKeyCount(String appName, String ruleKey, String time) {
        String key = buildKey(appName, ruleKey, time);
        return redis.llen(key);
    }

    public String buildKey(String appName, String ruleKey, long time) {
        return buildKey(appName, ruleKey, new SimpleDateFormat("yyyyMMddHH").format(new Date(time)));
    }

    public String buildKey(String appName, String ruleKey, String time) {
        return "hk:" + appName + ":" + ruleKey + ":" + time;
    }

    public String buildWorkerKey(String appName, String ruleKey, String time) {
        return "wk:" + appName + ":" + ruleKey + ":" + time;
    }

    public String buildLockKey(String appName, String ruleKey, String time) {
        return "L:" + appName + ":" + ruleKey + ":" + time;
    }

    public void setRedis(IRedis redis) {
        this.redis = redis;
    }
}
