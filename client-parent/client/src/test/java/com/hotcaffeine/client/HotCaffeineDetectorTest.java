package com.hotcaffeine.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HotCaffeineDetectorTest {

    HotCaffeine hotSpotCache;
    
    HotCaffeine hotSpotCache2;
    
    HotCaffeine hotSpotCache3;
    
    HotCaffeine prefixHotCaffeine;
    
    @Before
    public void init() {
        HotCaffeineDetector hotCaffeineDetector = new HotCaffeineDetector.Builder().endpoints("http://etcd.test.com:2379")
                .appName("core.user.hotcaffeine").build();
        hotCaffeineDetector.start();
        hotSpotCache = hotCaffeineDetector.build();
        
        hotSpotCache2 = hotCaffeineDetector.build("major");
        
        hotSpotCache3 = hotCaffeineDetector.build("minor", k->{
            return k + "-value";
        });
        
        prefixHotCaffeine = hotCaffeineDetector.buildPrefix();
    }
    
    @Test
    public void test() throws InterruptedException {
        String key = "api:user:123";
        while(true) {
            hotSpotCache.getValue(key, k->"{\"id\":123, \"name\":\"hotcaffeine\", \"age\": 1}");
            Thread.sleep(1);
        }
    }
    
    @Test
    public void testPrefix() throws InterruptedException {
        while(true) {
            prefixHotCaffeine.getValue("api:user", k->"v");
            Thread.sleep(10);
        }
    }
    
    @Test
    public void testHot() {
        String key = "test";
        for(int i = 0; i < Integer.MAX_VALUE; ++i) {
            hotSpotCache.getValue(key, k->"v");
        }
    }

    @Test
    public void testHot2() {
        new Thread(()->{
            String key = "test";
            for(int i = 0; i < Integer.MAX_VALUE; ++i) {
                String value = hotSpotCache2.getValue(key, k->"v");
                Assert.assertNull(value);
            }
        }).start();
        String key = "test";
        for(int i = 0; i < Integer.MAX_VALUE; ++i) {
            String value = hotSpotCache.getValue(key, k->"v");
            Assert.assertNull(value);
        }
    }
    
    @Test
    public void testHot3() {
        new Thread(()->{
            String key = "test";
            for(int i = 0; i < Integer.MAX_VALUE; ++i) {
                String value = hotSpotCache3.getValue(key, k->"v");
                if(value != null) {
                    Assert.assertEquals(key + "-value", value);
                }
            }
        }).start();
        new Thread(()->{
            String key = "test";
            for(int i = 0; i < Integer.MAX_VALUE; ++i) {
                String value = hotSpotCache2.getValue(key, k->"v");
                Assert.assertNull(value);
            }
        }).start();
        String key = "test";
        for(int i = 0; i < Integer.MAX_VALUE; ++i) {
            boolean hot = hotSpotCache.isHot(key);
            if(!hot) {
                System.out.println(i);
            }
        }
    }
}
