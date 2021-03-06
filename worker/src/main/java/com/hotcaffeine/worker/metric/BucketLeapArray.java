/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hotcaffeine.worker.metric;

/**
 * The fundamental data structure for metric statistics in a time span.
 *
 * @author jialiang.linjl
 * @author Eric Zhao
 * @see LeapArray
 */
public class BucketLeapArray extends LeapArray<Long> {
    
    // 总量
    private long totalCount;

    public BucketLeapArray(int sampleCount, int intervalInMs) {
        super(sampleCount, intervalInMs);
    }

    @Override
    public Long newEmptyBucket(long time) {
        return 0L;
    }

    @Override
    protected WindowWrap<Long> resetWindowTo(WindowWrap<Long> w, long startTime) {
        // Update the start time and reset value.
        w.resetTo(startTime);
        w.setValue(0L);
        return w;
    }

    /**
     * 计数并返回总量
     * 
     * @param count
     * @return
     */
    public long count(long count) {
        long timeMillis = System.currentTimeMillis();
        // 计数
        WindowWrap<Long> window = currentWindow(timeMillis);
        window.setValue(window.value() + count);
        // 总量计数
        totalCount += count;
        // 统计总量
        return leapArrayCount(timeMillis);
    }

    /**
     * qps
     * 
     * @return
     */
    public double qps() {
        return leapArrayCount(System.currentTimeMillis()) / getIntervalInSecond();
    }

    /**
     * 计算滑动窗口总量
     * 
     * @param timeMillis
     * @return
     */
    public long leapArrayCount(long timeMillis) {
        // 统计总量
        int size = array.length();
        long total = 0;
        for (int i = 0; i < size; i++) {
            WindowWrap<Long> windowWrap = array.get(i);
            if (windowWrap == null || isWindowDeprecated(timeMillis, windowWrap)) {
                continue;
            }
            total += windowWrap.value().longValue();
        }
        return total;
    }

    /**
     * 计算滑动窗口总量
     * 
     * @return
     */
    public long leapArrayCount() {
        return leapArrayCount(System.currentTimeMillis());
    }

    /**
     * 获取总量
     * @return
     */
    public long getTotalCount() {
        return totalCount;
    }
}
