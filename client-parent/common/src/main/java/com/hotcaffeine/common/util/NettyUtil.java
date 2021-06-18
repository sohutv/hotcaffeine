package com.hotcaffeine.common.util;

import java.net.SocketAddress;

import com.hotcaffeine.common.model.Message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/**
 * netty工具类
 * 
 * @author yongfeigao
 * @date 2021年1月7日
 */
public class NettyUtil {
    /**
     * netty的分隔符
     */
    public static final String DELIMITER = "$(* *)$";
    
    public static final ByteBuf DELIMITER_BUFFER = Unpooled.copiedBuffer(DELIMITER.getBytes());
    
    /**
     * 解析地址
     * @param channel
     * @return
     */
    public static String parseRemoteAddr(final Channel channel) {
        if (null == channel) {
            return "";
        }
        SocketAddress remote = channel.remoteAddress();
        final String addr = remote != null ? remote.toString() : "";

        if (addr.length() > 0) {
            int index = addr.lastIndexOf("/");
            if (index >= 0) {
                return addr.substring(index + 1);
            }
            return addr;
        }
        return "";
    }
    
    public static ByteBuf buildByteBuf(Message message) {
        return Unpooled.copiedBuffer((JsonUtil.toJSON(message) + DELIMITER).getBytes());
    }
    
    public static ByteBuf buildPongByteBuf() {
        return Unpooled.copiedBuffer((JsonUtil.toJSON(Message.PONG_MSG) + DELIMITER).getBytes());
    }
    
    public static ByteBuf buildPingByteBuf() {
        return Unpooled.copiedBuffer((JsonUtil.toJSON(Message.PING_MSG) + DELIMITER).getBytes());
    }
}
