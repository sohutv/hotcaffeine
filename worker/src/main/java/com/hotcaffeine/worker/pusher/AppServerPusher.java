package com.hotcaffeine.worker.pusher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.NettyUtil;
import com.hotcaffeine.worker.netty.processor.ClientChannelProcessor;

import io.netty.channel.group.ChannelGroup;

/**
 * 推送到各客户端服务器
 * 
 * @author wuweifeng wrote on 2020-02-24
 * @version 1.0
 */
@Component
public class AppServerPusher implements IPusher {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ClientChannelProcessor clientChannelProcessor;

    /**
     * 给客户端推key信息
     */
    @Override
    public void push(KeyCount keyCount) {
        // 内部消息处理
        if (keyCount.isInner()) {
            keyCount.getChannel().writeAndFlush(
                    NettyUtil.buildByteBuf(new Message(MessageType.RESPONSE_NEW_KEY, JsonUtil.toJSON(keyCount))));
            return;
        }
        // 正常消息处理
        ChannelGroup channelGroup = clientChannelProcessor.getChannelGroup(keyCount.getAppName());
        if (channelGroup == null) {
            logger.error("app:{} no channel", keyCount.getAppName());
            return;
        }
        Message message = new Message(MessageType.RESPONSE_NEW_KEY, JsonUtil.toJSON(keyCount));
        channelGroup.writeAndFlush(NettyUtil.buildByteBuf(message));
    }
}
