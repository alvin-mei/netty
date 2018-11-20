package io.netty.example.ex1.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.ex1.packets.HeartBeatResponsePacket;

/**
 * @author meijingling
 * @date 18/11/6
 */
@ChannelHandler.Sharable
public class HeartBeatRequestHandler extends SimpleChannelInboundHandler<HeartBeatResponsePacket> {
    public static final HeartBeatRequestHandler INSTANCE = new HeartBeatRequestHandler();

    private HeartBeatRequestHandler() {

    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HeartBeatResponsePacket heartBeatResponsePacket) throws Exception {
        System.out.println("server收到心跳.....");
        ctx.writeAndFlush(heartBeatResponsePacket);
    }


}
