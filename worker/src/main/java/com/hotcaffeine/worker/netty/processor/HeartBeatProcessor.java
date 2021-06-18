package com.hotcaffeine.worker.netty.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.channel.Channel;

/**
 * 心跳处理
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class HeartBeatProcessor implements IRequestProcessor {
    
    private Logger logger = LoggerFactory.getLogger(getClass()); 

    @Override
    public void process(Message message, Channel channel) {
        try {
            channel.writeAndFlush(NettyUtil.buildPongByteBuf()).sync();
        } catch (InterruptedException e) {
            logger.info("ignore {}", e.toString());
        }
    }

    @Override
    public MessageType messageType() {
        return MessageType.PING;
    }

}
