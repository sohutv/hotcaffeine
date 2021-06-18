package com.hotcaffeine.worker.netty.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hotcaffeine.common.model.MessageType;

/**
 * 请求处理器仓库
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class RequestProcessorRepository {

    private Map<MessageType, IRequestProcessor> requestProcessorMap = new HashMap<>();

    @Autowired
    private List<IRequestProcessor> requestProcessors;

    @PostConstruct
    public void init() {
        for (IRequestProcessor requestProcessor : requestProcessors) {
            requestProcessorMap.put(requestProcessor.messageType(), requestProcessor);
        }
    }

    /**
     * 获取请求处理器
     * @param messageType
     * @return
     */
    public IRequestProcessor getRequestProcessor(MessageType messageType) {
        return requestProcessorMap.get(messageType);
    }
}
