package io.netty.example.ex1.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.Buffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * @author meijingling
 * @date 18/11/2
 */
public class FirstClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("get msg from server:" + in.toString(Charset.forName("utf-8")));
    }

    // 这个方法会在客户端连接建立成功之后被调用
    @Override
    public void channelActive(ChannelHandlerContext context) {
        System.out.println(new Date() + ": 客户端写出数据");

        // 获取数据
        ByteBuf buf = getByteBuf(context);

        // 写数据
        context.channel().writeAndFlush(buf);
    }


    private ByteBuf getByteBuf(ChannelHandlerContext context) {
        // 获取二进制抽象 byteBuf
        ByteBuf byteBuf = context.alloc().buffer();

        byte[] bytes = "hello world111111111111".getBytes(Charset.forName("utf-8"));

        byteBuf.writeBytes(bytes);

        return byteBuf;
    }
}
