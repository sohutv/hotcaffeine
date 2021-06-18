package com.hotcaffeine.client.listener;

import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.NettyUtil;

import io.etcd.jetcd.shaded.com.google.common.eventbus.AllowConcurrentEvents;
import io.etcd.jetcd.shaded.com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;

/**
 * 不支持的消息类型
 * 
 * @author yongfeigao
 * @date 2021年4月12日
 */
public class UnsupportedMessageTypeListener {

    @Subscribe
    @AllowConcurrentEvents
    public void messageComing(UnsupportedMessageTypeEvent event) {
        Message msg = new Message(MessageType.UNSUPPORTED_TYPE,
                "unsupported type:" + event.getMsg().getMessageType());
        msg.setRequestId(event.getMsg().getRequestId());
        event.getChannel().writeAndFlush(NettyUtil.buildByteBuf(msg));
    }

    public static class UnsupportedMessageTypeEvent {
        private Channel channel;
        private Message msg;

        public UnsupportedMessageTypeEvent(Channel channel, Message msg) {
            this.channel = channel;
            this.msg = msg;
        }

        public Channel getChannel() {
            return channel;
        }

        public Message getMsg() {
            return msg;
        }
    }
}
