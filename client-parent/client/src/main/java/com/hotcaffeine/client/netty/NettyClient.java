package com.hotcaffeine.client.netty;

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.hotcaffeine.client.listener.ReceiveNewKeyListener.ReceiveNewKeyEvent;
import com.hotcaffeine.client.listener.RequestHotValueListener.RequestHotValueEvent;
import com.hotcaffeine.client.listener.UnsupportedMessageTypeListener.UnsupportedMessageTypeEvent;
import com.hotcaffeine.common.metric.NettyTrafficMetrics;
import com.hotcaffeine.common.model.ConsistentHashServer;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.ClientLogger;
import com.hotcaffeine.common.util.Constant;
import com.hotcaffeine.common.util.EventBusUtil;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * netty连接器
 *
 * @author wuweifeng wrote on 2019-11-05.
 */
public class NettyClient {
    // 地址->channel
    private final ConcurrentMap<String, Channel> channelTables = new ConcurrentHashMap<>();
    // 一致性hash地址
    private final ConsistentHashServer consistentHashServer = new ConsistentHashServer();

    private Bootstrap bootstrap;

    private String appName;
    
    private NettyTrafficMetrics nettyTrafficMetrics;
    
    public NettyClient(String appName) {
        this.appName = appName;
        nettyTrafficMetrics = new NettyTrafficMetrics();
    }

    /**
     * 启动
     */
    public void start() {
        bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(1)).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(nettyTrafficMetrics.getGlobalTrafficShapingHandler())
                                .addLast(new DelimiterBasedFrameDecoder(Constant.MAX_LENGTH, NettyUtil.DELIMITER_BUFFER))
                                .addLast(new StringDecoder())
                                .addLast(new IdleStateHandler(0, 0, 30))
                                .addLast(new NettyClientHandler());
                    }
                });
    }

    /**
     * 通道是否OK
     * 
     * @param channel
     * @return
     */
    private boolean channelIsOk(Channel channel) {
        return channel != null && channel.isActive();
    }

    /**
     * 连接地址
     * 
     * @param addressList
     */
    public synchronized void connect(Set<String> addresses) {
        // 1.无变更直接返回
        if (!changed(addresses)) {
            return;
        }
        ClientLogger.getLogger().info("worker changed! old:{}, new:{}", channelTables.keySet(), addresses);
        // 2.链接新地址
        for (String address : addresses) {
            if (channelIsOk(channelTables.get(address))) {
                continue;
            }
            String[] ss = address.split(":");
            try {
                ChannelFuture channelFuture = bootstrap.connect(ss[0], Integer.parseInt(ss[1])).sync();
                Channel channel = channelFuture.channel();
                channelTables.put(address, channel);
                consistentHashServer.add(address);
                ClientLogger.getLogger().info("connect:{}", address);
            } catch (Exception e) {
                ClientLogger.getLogger().error("worker:{} connect error:{}", address, e.toString());
            }
        }
        // 3.移除旧地址
        for (String address : channelTables.keySet()) {
            if (!addresses.contains(address)) {
                removeAddress(address, "etcd");
            }
        }
    }

    /**
     * 挑选地址
     * 
     * @param key
     * @return
     */
    public String choose(String key) {
        String address = consistentHashServer.select(key);
        if (address == null) {
            return null;
        }
        return address;
    }

    /**
     * writeAndFlush
     * 
     * @param address
     * @param msg
     * @return
     * @throws InterruptedException
     */
    public ChannelFuture writeAndFlush(String address, Message msg) throws InterruptedException {
        Channel channel = channelTables.get(address);
        if (!channelIsOk(channel)) {
            return null;
        }
        return channel.writeAndFlush(NettyUtil.buildByteBuf(msg)).sync();
    }

    /**
     * 地址是否有变更
     * 
     * @param addressList
     * @return
     */
    private boolean changed(Set<String> addresses) {
        if (channelTables.size() != addresses.size()) {
            return true;
        }
        for (String address : addresses) {
            if (!channelTables.containsKey(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 移除address
     * 
     * @param address
     */
    public void removeAddress(String address, String flag) {
        consistentHashServer.remove(address);
        Channel channel = channelTables.remove(address);
        if (channel != null) {
            channel.close();
        }
        ClientLogger.getLogger().warn("flag:{} remove channel:{}", flag, address);
    }

    /**
     * 关闭
     */
    public synchronized void shutdown() {
        for (Entry<String, Channel> entry : channelTables.entrySet()) {
            entry.getValue().close();
            ClientLogger.getLogger().info("close:{}", entry.getKey());
        }
        ClientLogger.getLogger().info("nettyClient shutdown");
    }

    /**
     * 获取地址
     * 
     * @return
     */
    public Set<String> getAddresses() {
        return channelTables.keySet();
    }

    /**
     * 获取hash地址
     * 
     * @return
     */
    public SortedMap<Long, String> getConsistentHashServer() {
        return consistentHashServer.getServerMap();
    }

    @ChannelHandler.Sharable
    public class NettyClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;

                if (idleStateEvent.state() == IdleState.ALL_IDLE) {
                    ctx.writeAndFlush(NettyUtil.buildPingByteBuf());
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ClientLogger.getLogger().info("channelActive:{}", NettyUtil.parseRemoteAddr(ctx.channel()));
            ctx.writeAndFlush(NettyUtil.buildByteBuf(new Message(MessageType.APP_NAME, appName)));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            String address = NettyUtil.parseRemoteAddr(ctx.channel());
            removeAddress(address, "netty");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ClientLogger.getLogger().warn("remoteAddr:{} error:{}", NettyUtil.parseRemoteAddr(ctx.channel()),
                    cause.toString());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, String message) {
            Message msg = JsonUtil.toBean(message, Message.class);
            if (MessageType.PONG == msg.getMessageType()) {
                return;
            }
            if (MessageType.RESPONSE_NEW_KEY == msg.getMessageType()) {
                KeyCount keyCount = JsonUtil.toBean(msg.getBody(), KeyCount.class);
                ClientLogger.getLogger().info("receive {} new key:{}", NettyUtil.parseRemoteAddr(context.channel()), msg);
                ReceiveNewKeyEvent event = new ReceiveNewKeyEvent(keyCount);
                event.setChannel(context.channel());
                EventBusUtil.post(event);
            } else if (MessageType.REQUEST_HOTKEY_VALUE == msg.getMessageType()) {
                String key = msg.getBody();
                ClientLogger.getLogger().info("receive requestHotValue key:{}", key);
                EventBusUtil.post(new RequestHotValueEvent(context.channel(), key, 
                        msg.getRequestId()));
            } else {
                ClientLogger.getLogger().info("unsupported msg type:{}", msg);
                EventBusUtil.post(new UnsupportedMessageTypeEvent(context.channel(), msg));
            }
        }
    }
}
