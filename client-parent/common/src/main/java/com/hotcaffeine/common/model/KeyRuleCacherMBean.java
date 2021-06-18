package com.hotcaffeine.common.model;

/**
 * mbean
 * 
 * @author yongfeigao
 * @date 2021年1月26日
 */
public interface KeyRuleCacherMBean {
    /**
     * 获取数据
     * @param key
     * @return
     */
    public String getValue(String key);
}
