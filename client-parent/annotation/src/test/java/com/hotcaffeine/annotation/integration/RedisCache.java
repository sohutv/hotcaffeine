package com.hotcaffeine.annotation.integration;

import org.springframework.stereotype.Component;

import com.hotcaffeine.annotation.HotCaffeineSensor;
import com.hotcaffeine.client.listener.IKeyListener;

@Component
public class RedisCache implements IKeyListener {

    @HotCaffeineSensor
    public String getValue(String key) {
        return key + "123";
    }

    @Override
    public Object hot(String key) {
        return getValue(key);
    }
}
