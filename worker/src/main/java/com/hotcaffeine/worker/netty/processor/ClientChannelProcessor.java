package com.hotcaffeine.worker.netty.processor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 客户端通道管理
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class ClientChannelProcessor implements IRequestProcessor {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<String, ChannelGroup> clientChannelMap = new ConcurrentHashMap<String, ChannelGroup>();

    /**
     * 添加channel
     * 
     * @param appName
     * @param channel
     * @return
     */
    public void process(Message message, Channel channel) {
        String appName = message.getBody();
        ChannelGroup channelGroup = clientChannelMap.computeIfAbsent(appName, k -> {
            return new DefaultChannelGroup(k, GlobalEventExecutor.INSTANCE);
        });
        channelGroup.add(channel);
        logger.info("add new channel, appName:{}, channel:{}, size:{}", appName, NettyUtil.parseRemoteAddr(channel),
                channelGroup.size());
    }

    @Override
    public MessageType messageType() {
        return MessageType.APP_NAME;
    }
    
    public ChannelGroup getChannelGroup(String appName) {
        return clientChannelMap.get(appName);
    }

    public ConcurrentMap<String, ChannelGroup> getClientChannelMap() {
        return clientChannelMap;
    }
}
