package cn.ipman.rpcman.core.provider.http;

import cn.ipman.rpcman.core.meta.InstanceMeta;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.logging.Level;

/**
 * Netty Server
 *
 * @Author IpMan
 * @Date 2024/3/24 16:01
 */
@Slf4j
public class HttpServer {

    EventLoopGroup boosGroup = new NioEventLoopGroup(3);
    EventLoopGroup workerGroup = new NioEventLoopGroup(1000);

    int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws Throwable {
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_BACKLOG, 128) // 连接队列大小
                .option(ChannelOption.TCP_NODELAY, true) // 关闭Nagle,即时传输
                .option(ChannelOption.SO_KEEPALIVE, true) // 支持长连接
                .option(ChannelOption.SO_REUSEADDR, true) // 共享端口
                .option(ChannelOption.SO_RCVBUF, 32 * 1024) // 操作缓冲区的大小
                .option(ChannelOption.SO_SNDBUF, 32 * 1024) // 发送缓冲区的大小
                .option(EpollChannelOption.SO_REUSEPORT, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        b.group(boosGroup, workerGroup).channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpServerCodec()); // request/response HTTP编解码
                        p.addLast(new HttpObjectAggregator(10 * 1024 * 1024)); // 传输内容最大长度
                        p.addLast(new HttpServerInvoker()); // 请求处理器
                    }
                });
        String ip = InetAddress.getLocalHost().getHostAddress();
        Channel ch = b.bind(ip, port).sync().channel();
        log.info("开启netty http服务器，监听地址和端口为 http://" + ip + ":" + port + '/');
        ch.closeFuture().sync();
    }

    public void stop() {
        boosGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
