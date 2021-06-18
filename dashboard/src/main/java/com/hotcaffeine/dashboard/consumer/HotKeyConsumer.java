package com.hotcaffeine.dashboard.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.util.MemoryMQ.MemoryMQConsumer;
import com.hotcaffeine.dashboard.service.HotKeyService;

/**
 * 热键保存
 * 
 * @author yongfeigao
 * @date 2021年5月8日
 */
@Component
public class HotKeyConsumer implements MemoryMQConsumer<KeyCount> {
    
    @Autowired
    private HotKeyService hotKeyService;

    @Override
    public void consume(KeyCount keyCount) throws Exception {
        hotKeyService.putKeyCount(keyCount);
    }
}
