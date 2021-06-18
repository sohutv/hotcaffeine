package com.hotcaffeine.worker.pusher;

import com.hotcaffeine.common.model.KeyCount;

/**
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
public interface IPusher {
    void push(KeyCount keyCount);
}
