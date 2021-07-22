package com.hotcaffeine.client.worker;

import com.hotcaffeine.common.util.ClientLogger;
/**
 * worker状态
 * 
 * @author yongfeigao
 * @date 2021年7月21日
 */
public class WorkerStat {
    // 最大响应时间
    public static final int MAX_RESPONSE_TIME_IN_MILLIS = 500;

    // 时间消耗数组
    private TimeConsumed[] timeConsumedArray;
    // 数组索引
    private long index;

    // worker
    private String worker;

    // 是否健康
    private boolean healthy = true;

    public WorkerStat(String worker) {
        this.worker = worker;
        this.timeConsumedArray = new TimeConsumed[7];
    }

    public void addTimeConsumed(TimeConsumed timeConsumed) {
        timeConsumedArray[(int) (index++ % timeConsumedArray.length)] = timeConsumed;
    }

    public void setConsumed(String key) {
        for (TimeConsumed timeConsumed : timeConsumedArray) {
            if (timeConsumed == null) {
                continue;
            }
            if (!timeConsumed.isDetected() && key.equals(timeConsumed.getKey())) {
                timeConsumed.setConsumed(System.currentTimeMillis() - timeConsumed.getStart());
                return;
            }
        }
    }

    /**
     * 健康判定，每30秒检测6次，如下情况被认定为不健康： 
     * 1.没有检测。 
     * 2.一半及以上的检测无响应。 
     * 3.一半及以上的检测超时。
     * 4.检测平均耗时超过阈值。
     * 
     * @return
     */
    public boolean healthy() {
        int totalSize = 0;
        int invalidSize = 0;
        int timeoutSize = 0;
        long totalConsumed = 0;
        for (TimeConsumed timeConsumed : timeConsumedArray) {
            if (timeConsumed == null || timeConsumed.isDetected()) {
                continue;
            }
            ++totalSize;
            // 没有响应的暂时不处理
            if (timeConsumed.getConsumed() < 0) {
                ++invalidSize;
            } else {
                // 超时
                if (timeConsumed.getConsumed() > MAX_RESPONSE_TIME_IN_MILLIS) {
                    ++timeoutSize;
                    ClientLogger.getLogger().warn("workerHealthDetector:{} key:{} hot use:{}ms too long!",
                            worker, timeConsumed.getKey(), timeConsumed.getConsumed());
                }
                totalConsumed += timeConsumed.getConsumed();
            }
            timeConsumed.setDetected(true);
        }
        // 没有检测数据
        if (totalSize == 0) {
            ClientLogger.getLogger().warn("workerHealthDetector:{} unhealthy! no request", worker);
            return setHealthy(false);
        }
        // 一半的检测没有响应数据
        if (totalSize > 1 && invalidSize / (double) totalSize >= 0.5) {
            ClientLogger.getLogger().warn("workerHealthDetector:{} unhealthy! invalid:{} total:{}", worker,
                    invalidSize, totalSize);
            return setHealthy(false);
        }
        // 平均耗时超过阈值
        double avgConsumed = totalConsumed / (double) totalSize;
        if (avgConsumed >= MAX_RESPONSE_TIME_IN_MILLIS) {
            ClientLogger.getLogger().warn("workerHealthDetector:{} unhealthy! avgConsumed:{}ms total:{}",
                    worker, format(avgConsumed), totalSize);
            return setHealthy(false);
        }
        // 一半的检测超时
        if (timeoutSize / (double) totalSize >= 0.5) {
            ClientLogger.getLogger().warn("workerHealthDetector:{} unhealthy! timeout:{} total:{}", worker,
                    timeoutSize, totalSize);
            return setHealthy(false);
        }
        ClientLogger.getLogger().info("workerHealthDetector:{} healthy, avgConsumed:{}ms",
                worker, format(avgConsumed));
        return setHealthy(true);
    }

    /**
     * 保留微秒
     * 
     * @param value
     * @return
     */
    private double format(double value) {
        long v = (long) (value * 1000);
        return v / 1000D;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public boolean setHealthy(boolean healthy) {
        this.healthy = healthy;
        return this.healthy;
    }

    public TimeConsumed[] getTimeConsumedArray() {
        return timeConsumedArray;
    }

    public long getIndex() {
        return index;
    }
}
