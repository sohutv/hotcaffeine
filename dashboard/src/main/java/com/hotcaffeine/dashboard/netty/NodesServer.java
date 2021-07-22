package com.hotcaffeine.dashboard.netty;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.metric.NettyTrafficMetrics;
import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.KeyCount;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.model.MessageType;
import com.hotcaffeine.common.util.Constant;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.MemoryMQ;
import com.hotcaffeine.common.util.NettyUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 该server用于给各个worker实例连接用。
 *
 * @author wuweifeng wrote on 2019-11-05.
 */
@Component
public class NodesServer implements Destroyable {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    
    private NettyTrafficMetrics nettyTrafficMetrics;
    
    @Autowired
    private MemoryMQ<KeyCount> hotKeyMemoryMQ;
    
    @Value("${dashbord.port:11112}")
    private int dashboardPort;
    
    @PostConstruct
    public void startNettyServer() {
        nettyTrafficMetrics = new NettyTrafficMetrics();
        //boss单线程
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    //保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //出来网络io事件，如记录日志、对消息编解码等
                    .childHandler(new ChildChannelHandler());
            //绑定端口，同步等待成功
            bootstrap.bind(dashboardPort).sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("dashboard netty server start failure");
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    /**
     * handler类
     */
    private class ChildChannelHandler extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) {
            NodesServerHandler serverHandler = new NodesServerHandler();
            ch.pipeline()
                    .addLast(nettyTrafficMetrics.getGlobalTrafficShapingHandler())
                    .addLast(new DelimiterBasedFrameDecoder(Constant.MAX_LENGTH, NettyUtil.DELIMITER_BUFFER))
                    .addLast(new StringDecoder())
                    .addLast(serverHandler);
        }
    }
    
    private class NodesServerHandler extends SimpleChannelInboundHandler<String> {

        private Logger logger = LoggerFactory.getLogger(getClass());
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) {
            if (StringUtils.isEmpty(message)) {
                return;
            }
            try {
                Message msg = JsonUtil.toBean(message, Message.class);
                if (MessageType.PING == msg.getMessageType()) {
                    ctx.channel().writeAndFlush(NettyUtil.buildPongByteBuf()).sync();
                } else if (MessageType.REQUEST_HOT_KEY == msg.getMessageType()) {
                    List<KeyCount> list = JsonUtil.toList(msg.getBody(), KeyCount.class);
                    for (KeyCount keyCount : list) {
                        hotKeyMemoryMQ.offer(keyCount);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            logger.warn("remoteAddr:{} error:{}", NettyUtil.parseRemoteAddr(ctx.channel()), cause.toString());
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.close();
            super.channelInactive(ctx);
        }

    }

    @Override
    public void destroy() throws Exception {
        shutdown();
    }

    @Override
    public int order() {
        return 50;
    }

}
