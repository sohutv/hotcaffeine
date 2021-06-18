package com.hotcaffeine.common.model;

import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.common.cache.LocalCache;

/**
 * cache规则
 * 
 * @author yongfeigao
 * @date 2021年3月18日
 */
public class CacheRule {
    // 缓存名
    private String name;
    // 缓存时间
    private int duration;
    // 缓存大小
    private int size;

    // 本地缓存
    private LocalCache<ValueModel> cache;
    
    /**
     * 更新
     * @param keyRule
     */
    public boolean update(CacheRule keyCache, boolean initCache) {
        // 无需更新缓存返回
        if(keyCache.getSize() == size && keyCache.getDuration() == duration) {
            return false;
        }
        // 缓存需要更新
        this.size = keyCache.getSize();
        this.duration = keyCache.getDuration();
        if (initCache) {
            initCache();
        }
        return true;
    }
    
    /**
     * 初始化缓存
     */
    public void initCache() {
        cache = new CaffeineCache<>(name, size, duration);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public LocalCache<ValueModel> getCache() {
        return cache;
    }

    @Override
    public String toString() {
        return "CacheRule [name=" + name + ", duration=" + duration + ", size=" + size + "]";
    }
}
