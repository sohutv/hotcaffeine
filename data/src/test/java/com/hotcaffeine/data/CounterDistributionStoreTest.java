package com.hotcaffeine.data;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.hotcaffeine.data.metric.CounterDistribution;
import com.hotcaffeine.data.metric.KeyCounterData;
import com.hotcaffeine.data.store.PooledRedis;

public class CounterDistributionStoreTest {

    @Test
    public void test() {
        PooledRedis redis = new PooledRedis();
        CounterDistributionStore counterDistributionStore = new CounterDistributionStore();
        counterDistributionStore.setRedis(redis);
        
        String appName = "core.api.hotcaffeine";
        String ruleKey = "major";
        long time = System.currentTimeMillis();
        
        CounterDistribution counterDistribution = new CounterDistribution();
        counterDistribution.incr(4, 3);
        counterDistribution.incr(10, 2);
        counterDistribution.incr(1, 1);
        
        Long rst = counterDistributionStore.store(appName, ruleKey, time, counterDistribution);
        Assert.assertNotNull(rst);
        
        List<KeyCounterData> list = counterDistributionStore.query(appName, ruleKey, time - 120 * 1000);
        Assert.assertEquals(3, list.get(0).getDistributionMap().size());
    }

}
