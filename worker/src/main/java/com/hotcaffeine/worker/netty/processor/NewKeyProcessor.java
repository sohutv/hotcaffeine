package com.hotcaffeine.worker.netty.processor;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.MemoryMQGroup;
import com.hotcaffeine.common.util.MetricsUtil;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.channel.Channel;

/**
 * 新key处理
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class NewKeyProcessor implements IRequestProcessor {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${netty.timeOut}")
    private int nettyTimeOut;

    // 内存mq
    @Resource
    private MemoryMQGroup<KeyCount> memoryMQGroup;

    @Override
    public void process(Message message, Channel channel) {
        MetricsUtil.incrReceiveTimes();
        List<KeyCount> models = JsonUtil.toList(message.getBody(), KeyCount.class);
        long timeOut = System.currentTimeMillis() - message.getCreateTime();
        // 客户端超时仅警告，不拦截，防止客户端时间不准导致误删
        if (timeOut > nettyTimeOut) {
            logger.warn("app:{} creatTime:{} timeout:{} from addr:{}", message.getAppName(), message.getCreateTime(),
                    timeOut, NettyUtil.parseRemoteAddr(channel));
        }
        MetricsUtil.incrReceiveKeys(models.size());
        for (KeyCount keyCount : models) {
            keyCount.setAppName(message.getAppName());
            if (keyCount.isInner()) {
                keyCount.setChannel(channel);
            }
            if (!memoryMQGroup.offer(keyCount)) {
                logger.warn("offer:{} failed, maybe full", keyCount);
            }
        }
    }

    @Override
    public MessageType messageType() {
        return MessageType.REQUEST_NEW_KEY;
    }
}
