package com.hotcaffeine.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.hotcaffeine.data.metric.CounterDistribution;
import com.hotcaffeine.data.metric.KeyCounterData;
import com.hotcaffeine.data.store.IRedis;

/**
 * 调用量分布保存
 * 
 * @author yongfeigao
 * @date 2021年3月4日
 */
public class CounterDistributionStore {

    private IRedis redis;

    /**
     * 存储key调用分布
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @param counterDistribution
     * @return
     */
    public Long store(String appName, String ruleKey, long time, CounterDistribution counterDistribution) {
        String key = buildKey(appName, ruleKey, time);
        KeyCounterData sortedKeyCounter = new KeyCounterData(counterDistribution.getDistributionMap(),
                counterDistribution.getTotalCount(), new SimpleDateFormat("mm").format(new Date(time)));
        Long rst = redis.lpush(key, JSON.toJSONString(sortedKeyCounter));
        Long ttl = redis.ttl(key);
        if (ttl == -1) {
            // 三天过期
            redis.expire(key, 3 * 24 * 60 * 60);
        }
        return rst;
    }

    /**
     * 查询key调用分布
     * 
     * @param appName
     * @param ruleKey
     * @param time
     * @return
     */
    public List<KeyCounterData> query(String appName, String ruleKey, long time) {
        String key = buildKey(appName, ruleKey, time);
        List<String> list = redis.lrange(key, 0, -1);
        if (list == null || list.size() == 0) {
            return null;
        }
        return list.stream().map(s -> JSON.parseObject(s, KeyCounterData.class)).collect(Collectors.toList());
    }

    public String buildKey(String appName, String ruleKey, long time) {
        return "d:" + appName + ":" + ruleKey + ":" + new SimpleDateFormat("yyyyMMddHH").format(new Date(time));
    }

    public void setRedis(IRedis redis) {
        this.redis = redis;
    }
}
