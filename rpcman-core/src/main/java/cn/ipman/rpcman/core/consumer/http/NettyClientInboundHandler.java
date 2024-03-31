package cn.ipman.rpcman.core.consumer.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;


/**
 * Description for this class
 *
 * @Author IpMan
 * @Date 2024/3/31 15:57
 */
public class NettyClientInboundHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse fullHttpResponse) throws Exception {
        ByteBuf buf = fullHttpResponse.content();
        System.out.println(buf.toString(CharsetUtil.UTF_8));
    }
}
