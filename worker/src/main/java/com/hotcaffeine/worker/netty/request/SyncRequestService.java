package com.hotcaffeine.worker.netty.request;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.hotcaffeine.common.cache.CaffeineCache;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.util.NettyUtil;
import com.hotcaffeine.worker.netty.processor.ClientChannelProcessor;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;

/**
 * 同步请求服务
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 * @param <T>
 */
@Component
public class SyncRequestService<T> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private CaffeineCache<SyncRequest<T>> caffeineCache;

    @Autowired
    private ClientChannelProcessor clientChannelProcessor;

    public SyncRequestService() {
        long maximumSize = 1000;
        Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(maximumSize)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .recordStats();
        caffeineCache = new CaffeineCache<>("syncRequest", caffeine, TimeUnit.MINUTES.toMillis(1L), maximumSize);
    }

    /**
     * 向app所有实例发送请求
     * @param appName
     * @param message
     * @param timeout
     * @return
     */
    public Map<String, T> request(String appName, Message message, long timeout) {
        ChannelGroup channelGroup = clientChannelProcessor.getChannelGroup(appName);
        if (channelGroup == null) {
            return null;
        }
        Map<String, T> resultMap = new HashMap<>();
        channelGroup.forEach(channel -> {
            T result = request(channel, message, timeout);
            if (result != null) {
                resultMap.put(NettyUtil.parseRemoteAddr(channel), result);
            }
        });
        return resultMap;
    }

    /**
     * 发送请求
     * @param channel
     * @param message
     * @param timeout
     * @return
     */
    public T request(Channel channel, Message message, long timeout) {
        // 初始化唯一请求的id
        String requestId = String.valueOf(message.initRequestId());
        // 构建同步请求
        SyncRequest<T> syncRequest = new SyncRequest<>();
        try {
            // 放入缓存
            caffeineCache.set(requestId, syncRequest);
            channel.writeAndFlush(NettyUtil.buildByteBuf(message)).addListener(future -> {
                if (!future.isSuccess()) {
                    syncRequest.setResponse(null);
                    // caffeineCache.delete(requestId);
                    logger.warn("send a sync request to channel:{} failed.", NettyUtil.parseRemoteAddr(channel));
                }
            });
            try {
                return syncRequest.get(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.warn("ignore channel:{} sync request interrupted", NettyUtil.parseRemoteAddr(channel), e.toString());
            }
        } finally {
            caffeineCache.delete(requestId);
        }
        return null;
    }
    
    /**
     * 设置响应
     * @param message
     * @param channel
     */
    @SuppressWarnings("unchecked")
    public void response(Message message, Channel channel) {
        String requestId = String.valueOf(message.getRequestId());
        SyncRequest<T> syncRequest = caffeineCache.get(requestId);
        if (syncRequest == null) {
            logger.warn("receive response, but not matched any request, channel:{}, id:{}, type:{}",
                    NettyUtil.parseRemoteAddr(channel), requestId, message.getMessageType());
            return;
        }
        syncRequest.setResponse((T) message.getBody());
    }
}
