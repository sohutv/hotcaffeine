package com.hotcaffeine.worker.netty.server;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.hotcaffeine.common.metric.NettyTrafficMetrics;
import com.hotcaffeine.common.model.Destroyable;
import com.hotcaffeine.common.model.Message;
import com.hotcaffeine.common.util.Constant;
import com.hotcaffeine.common.util.JsonUtil;
import com.hotcaffeine.common.util.NettyUtil;
import com.hotcaffeine.worker.netty.processor.IRequestProcessor;
import com.hotcaffeine.worker.netty.processor.RequestProcessorRepository;

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
 * worker服务器
 * 
 * @author yongfeigao
 * @date 2021年4月9日
 */
@Component
public class WorkerServer implements Destroyable {
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Value("${netty.port}")
    private int port;
    
    @Resource
    private RequestProcessorRepository requestProcessorRepository;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private NettyTrafficMetrics nettyTrafficMetrics;

    @PostConstruct
    public void start() throws Exception {
        nettyTrafficMetrics = new NettyTrafficMetrics();
        //boss单线程
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
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
        bootstrap.bind(port).sync();
        logger.info("worker server start at:{}", port);
    }
    
    @Override
    public void destroy() throws Exception {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public int order() {
        return 20;
    }

    /**
     * handler类
     */
    private class ChildChannelHandler extends ChannelInitializer<Channel> {

        @Override
        protected void initChannel(Channel ch) {
            ch.pipeline()
                    .addLast(nettyTrafficMetrics.getGlobalTrafficShapingHandler())
                    .addLast(new DelimiterBasedFrameDecoder(Constant.MAX_LENGTH, NettyUtil.DELIMITER_BUFFER))
                    .addLast(new StringDecoder())
                    .addLast(new WorkerServerChannleHandler());
        }
    }
    
    /**
     * channel处理
     * 
     * @author yongfeigao
     * @date 2021年4月9日
     */
    private class WorkerServerChannleHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) {
            if (StringUtils.isEmpty(message)) {
                return;
            }
            Message msg = JsonUtil.toBean(message, Message.class);
            IRequestProcessor requestProcessor = requestProcessorRepository.getRequestProcessor(msg.getMessageType());
            if (requestProcessor == null) {
                logger.error("no processor, type:{}", msg.getMessageType());
                return;
            }
            requestProcessor.process(msg, ctx.channel());
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
            logger.info("channelInactive:{}", NettyUtil.parseRemoteAddr(ctx.channel()));
            ctx.close();
            super.channelInactive(ctx);
        }
    }
}
