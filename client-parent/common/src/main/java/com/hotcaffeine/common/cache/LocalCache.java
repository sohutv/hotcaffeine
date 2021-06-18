package com.hotcaffeine.common.cache;

import com.github.benmanes.caffeine.cache.Cache;

/**
 * @author wuweifeng wrote on 2020-02-21
 * @version 1.0
 */
public interface LocalCache<T> {

    T get(String key);

    T get(String key, T defaultValue);

    void delete(String key);

    void set(String key, T value);

    void removeAll();
    
    Cache<String, T> getCache();
}
