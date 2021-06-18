package com.hotcaffeine.annotation;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.hotcaffeine.annotation.integration.AopConfiguration;
import com.hotcaffeine.annotation.integration.RedisCache;

@ContextConfiguration(classes = {HotCaffeineAspectTest.class, AopConfiguration.class})
public class HotCaffeineAspectTest extends AbstractJUnit4SpringContextTests {
    
    @Autowired
    private RedisCache redisCache;

    @Test
    public void test() {
        String p = "p";
        String value = redisCache.getValue(p);
        Assert.assertNotNull(value);
    }

    @Test
    public void testHot() {
        String p = "p";
        for(int i = 0; i < Integer.MAX_VALUE; ++i) {
            String value = redisCache.getValue(p);
            Assert.assertNotNull(value);
        }
    }
}
