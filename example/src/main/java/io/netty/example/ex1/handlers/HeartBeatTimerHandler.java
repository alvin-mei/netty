package io.netty.example.ex1.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.example.ex1.packets.HeartBeatRequestPacket;

import java.util.concurrent.TimeUnit;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class HeartBeatTimerHandler extends ChannelInboundHandlerAdapter {
    private static final int HEARTBEAT_INTERVAL = 5;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        _scheduleSendHeartBeat(ctx);
        super.channelActive(ctx);
    }

    private void _scheduleSendHeartBeat(ChannelHandlerContext ctx) {
        ctx.executor().schedule(new Runnable() {
            @Override
            public void run() {
                if(ctx.channel().isActive()) {
                    System.out.println("client send heart beat");
                    ctx.writeAndFlush(new HeartBeatRequestPacket());
                    _scheduleSendHeartBeat(ctx);
                }
            }
        }, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
}
