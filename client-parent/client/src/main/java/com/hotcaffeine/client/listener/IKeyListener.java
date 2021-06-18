package com.hotcaffeine.client.listener;

/**
 * 键通知
 * 
 * @author yongfeigao
 * @date 2021年1月15日
 */
public interface IKeyListener {
    
    /**
     * key变热时通知，只通知一次，不会重复通知
     * 如果需要缓存hot(String key)的返回值，请将needCacheValue返回为true
     * 尽量一秒内返回
     * @param key
     * @return 需要缓存的值
     */
    public Object hot(String key);
    
    /**
     * 是否需要缓存值
     * @return 默认缓存
     */
    default boolean needCacheValue() {
        return true;
    } 
}
