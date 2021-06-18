package com.hotcaffeine.common.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * netty通信消息
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public class Message {
    public static final AtomicLong ID_GENERATOR = new AtomicLong();
    public static final Message PING_MSG = new Message(MessageType.PING, "ping");
    public static final Message PONG_MSG = new Message(MessageType.PONG, "pong");
    
    private long requestId;
    
    private String appName;

    private MessageType messageType;
    
    private long createTime;

    private String body;

    public Message(MessageType messageType, String body) {
        this(null, messageType, body, 0);
    }

    public Message(String appName, MessageType messageType, String body, long createTime) {
        this.appName = appName;
        this.messageType = messageType;
        this.body = body;
        this.createTime = createTime;
    }
    
    public Message() {
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getBody() {
        return body;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public void setBody(String body) {
        this.body = body;
    }
    
    public long initRequestId() {
        this.requestId = ID_GENERATOR.incrementAndGet();
        return requestId;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "Message [requestId=" + requestId + ", appName=" + appName + ", messageType=" + messageType
                + ", createTime=" + createTime + ", body=" + body + "]";
    }
}
