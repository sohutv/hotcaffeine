package com.hotcaffeine.worker.netty.processor;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;

import io.netty.channel.Channel;

/**
 * 请求处理器
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
public interface IRequestProcessor {
    /**
     * 处理请求
     * @param message
     * @param channel
     */
    void process(Message message, Channel channel);
    
    /**
     * 消息类型
     * @return
     */
    MessageType messageType();
}
