package com.hotcaffeine.worker.netty.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.worker.netty.request.SyncRequestService;

import io.netty.channel.Channel;

/**
 * 不支持的响应
 * 
 * @author yongfeigao
 * @date 2021年4月12日
 */
@Component
public class UnsupportedResponseProcessor implements IRequestProcessor {

    @Autowired
    private SyncRequestService<String> syncRequestService;
    
    @Override
    public void process(Message message, Channel channel) {
        syncRequestService.response(message, channel);
    }

    @Override
    public MessageType messageType() {
        return MessageType.UNSUPPORTED_TYPE;
    }
}
