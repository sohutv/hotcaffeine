package com.hotcaffeine.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hotcaffeine.data.metric.HotKey;
import com.hotcaffeine.data.metric.TopHotKey;
import com.hotcaffeine.data.store.PooledRedis;

public class TopHotKeyStoreTest {

    private static long time;
    private static CounterDistributionStore counterDistributionStore;
    private static TopHotKeyStore topHotKeyStore;

    private String appName = "core.api.hotcaffeine";
    private String ruleKey = "major";
    private static final String timeString = "202103091702";
    private static final String timeHourString = "2021030917";

    @BeforeClass
    public static void init() throws ParseException {
        time = new SimpleDateFormat("yyyyMMddHHmm").parse(timeString).getTime();
        PooledRedis redis = new PooledRedis();
        counterDistributionStore = new CounterDistributionStore();
        counterDistributionStore.setRedis(redis);
        topHotKeyStore = new TopHotKeyStore();
        topHotKeyStore.setRedis(redis);
    }

    @Test
    public void testStoreWorkerData() {
        TopHotKey topHotKey = buildTopHotKey();
        topHotKeyStore.storeWorkerData(appName, ruleKey, time, topHotKey);
    }

    @Test
    public void testMerge() {
        topHotKeyStore.merge(appName, ruleKey, timeString, 10);
    }

    @Test
    public void test() {
        TopHotKey topHotKey = buildTopHotKey();
        topHotKeyStore.store(appName, ruleKey, timeString, topHotKey);
    }

    @Test
    public void TestQueryTop() throws ParseException {
        List<TopHotKey> list = topHotKeyStore.queryTopHotKey(appName, ruleKey,
                new SimpleDateFormat("yyyyMMddHH").parse(timeHourString).getTime());
        Assert.assertNotNull(list);
    }

    @Test
    public void TestQueryHotKey() throws ParseException {
        List<HotKey> list = topHotKeyStore.queryHotKey(appName, ruleKey, timeString, 0, 10);
        System.out.println(list);
        Assert.assertNotNull(list);
    }

    public TopHotKey buildTopHotKey() {
        TopHotKey topHotKey = new TopHotKey(100);
        for (int i = 0; i < 10000; ++i) {
            topHotKey.add(new HotKey(i, i, "akey" + i));
        }
        return topHotKey;
    }

}
