package com.hotcaffeine.common.model;

/**
 * @author wuweifeng wrote on 2020-01-06
 * @version 1.0
 */
public enum MessageType {
    APP_NAME((byte) 1),
    REQUEST_NEW_KEY((byte) 2),
    RESPONSE_NEW_KEY((byte) 3),
    REQUEST_HOT_KEY((byte) 8), //热key，worker->dashboard
    PING((byte) 4), 
    PONG((byte) 5),
    REQUEST_HOTKEY_VALUE((byte) 6),
    RESPONSE_HOTKEY_VALUE((byte) 7),
    UNSUPPORTED_TYPE((byte) 100),
    ;

    private byte type;

    MessageType(byte type) {
        this.type = type;
    }

    public byte getType() {
        return type;
    }

    public static MessageType get(byte type) {
        for (MessageType value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }
}