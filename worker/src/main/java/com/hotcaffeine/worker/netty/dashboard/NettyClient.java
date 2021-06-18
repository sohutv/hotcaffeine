package com.hotcaffeine.worker.netty.dashboard;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.Constant;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
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
    private Logger logger = LoggerFactory.getLogger(getClass());
    private static final NettyClient nettyClient = new NettyClient();

    private Bootstrap bootstrap;
    
    /**
     * channel
     */
    public volatile Channel channel;

    public void flushToDashboard(List<KeyCount> keyCountList) {
        if (channel == null) {
            logger.warn("cannot flush size:{}, channel is null", keyCountList.size());
            return;
        }
        String message = JsonUtil.toJSON(keyCountList);
        channel.writeAndFlush(NettyUtil.buildByteBuf(new Message(MessageType.REQUEST_HOT_KEY, message)));
    }


    public static NettyClient getInstance() {
        return nettyClient;
    }

    private NettyClient() {
        if (bootstrap == null) {
            bootstrap = initBootstrap();
        }
    }

    private Bootstrap initBootstrap() {
        //少线程
        EventLoopGroup group = new NioEventLoopGroup(2);

        Bootstrap bootstrap = new Bootstrap();
        NettyClientHandler nettyClientHandler = new NettyClientHandler();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new DelimiterBasedFrameDecoder(Constant.MAX_LENGTH, NettyUtil.DELIMITER_BUFFER))
                                .addLast(new StringDecoder())
                                //10秒没消息时，就发心跳包过去
                                .addLast(new IdleStateHandler(0, 0, 30))
                                .addLast(nettyClientHandler);
                    }
                });
        return bootstrap;
    }

    public synchronized void connect(String address) {
        if (channel != null) {
            return;
        }
        String[] ss = address.split(":");
        try {
            ChannelFuture channelFuture = bootstrap.connect(ss[0], Integer.parseInt(ss[1])).sync();
            channel = channelFuture.channel();
        } catch (Exception e) {
            channel = null;
            logger.error("cannot connect:{} error:{}", address, e.toString());
        }
    }

    public synchronized void disConnect() {
        logger.info("disConnect channel:{}", channel);
        channel = null;
    }
    
    @ChannelHandler.Sharable
    public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

        private Logger logger = LoggerFactory.getLogger(getClass());

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
            logger.info("channelActive:{}", NettyUtil.parseRemoteAddr(ctx.channel()));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            NettyClient.getInstance().disConnect();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) {
            Message msg = JsonUtil.toBean(message, Message.class);
            if (MessageType.PONG == msg.getMessageType()) {
                if(logger.isDebugEnabled()) {
                    logger.debug("channel:{} heart beat", NettyUtil.parseRemoteAddr(ctx.channel()));
                }
            }
        }
    }
}
