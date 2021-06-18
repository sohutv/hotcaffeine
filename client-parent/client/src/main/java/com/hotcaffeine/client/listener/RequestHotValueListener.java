package com.hotcaffeine.client.listener;

import com.hotcaffeine.common.cache.LocalCache;
import com.hotcaffeine.common.model.KeyRule;
import com.hotcaffeine.common.model.KeyRuleCacher;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.model.ValueModel;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.NettyUtil;

import io.etcd.jetcd.shaded.com.google.common.eventbus.AllowConcurrentEvents;
import io.etcd.jetcd.shaded.com.google.common.eventbus.Subscribe;
import io.netty.channel.Channel;

/**
 * 请求热键值监听
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
public class RequestHotValueListener {

    private KeyRuleCacher keyRuleCacher;
    
    public RequestHotValueListener(KeyRuleCacher keyRuleCacher) {
        this.keyRuleCacher = keyRuleCacher;
    }

    @Subscribe
    @AllowConcurrentEvents
    public void hotValue(RequestHotValueEvent event) {
        String key = event.getKey();
        KeyRule keyRule = keyRuleCacher.findRule(key);
        ValueModel valueModel = null;
        if (keyRule != null) {
            LocalCache<ValueModel> localCache = keyRuleCacher.getCache(keyRule);
            if (localCache != null) {
                String realKey = keyRule.stripRuleKey(key);
                valueModel = localCache.get(realKey);
            } else {
                ClientLogger.getLogger().warn("key:{} no cache", key);
            }
        } else {
            ClientLogger.getLogger().warn("key:{} no keyRule", key);
        }
        Message msg = new Message(MessageType.RESPONSE_HOTKEY_VALUE, JsonUtil.toJSON(valueModel));
        msg.setRequestId(event.getRequestId());
        event.getChannel().writeAndFlush(NettyUtil.buildByteBuf(msg));
    }
    
    public static class RequestHotValueEvent {
        private Channel channel;
        private String key;
        private long requestId;

        public RequestHotValueEvent(Channel channel, String key, long requestId) {
            this.channel = channel;
            this.key = key;
            this.requestId = requestId;
        }

        public long getRequestId() {
            return requestId;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getKey() {
            return key;
        }
    }
}
