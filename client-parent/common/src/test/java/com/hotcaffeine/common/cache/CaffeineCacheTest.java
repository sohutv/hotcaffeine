package com.hotcaffeine.common.cache;

import org.junit.Assert;
import org.junit.Test;

public class CaffeineCacheTest {

    @Test
    public void test() {
        CaffeineCache<Object> cache = new CaffeineCache<>("test", 1024, 60);
        Assert.assertNotNull(cache);
        cache = new CaffeineCache<>("test", 1024, 60);
    }

}
