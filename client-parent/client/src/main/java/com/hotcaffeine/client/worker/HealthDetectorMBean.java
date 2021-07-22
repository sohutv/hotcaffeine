package com.hotcaffeine.client.worker;

import java.util.Map;

/**
 * worker状态 mbean
 * 
 * @author yongfeigao
 * @date 2021年7月21日
 */
public interface HealthDetectorMBean {
    
    /**
     * 获取worker状态
     * @return
     */
    public Map<String, WorkerStat> getWorkerStat();
}
