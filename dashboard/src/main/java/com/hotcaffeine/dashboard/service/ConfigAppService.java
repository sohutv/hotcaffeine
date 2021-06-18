package com.hotcaffeine.dashboard.service;

import java.util.List;

/**
 * 配置的app服务
 * 
 * @author yongfeigao
 * @date 2021年6月8日
 */
public interface ConfigAppService {
    default List<String> getApp(String userName) {
        return null;
    }

    default void refresh() {
        return;
    }
    
    public static class DefaultConfigAppService implements ConfigAppService {
    }
}
