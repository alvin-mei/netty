package io.netty.example.ex1.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.example.ex1.packets.Packet;
import io.netty.example.ex1.utils.PacketCodeC;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author meijingling
 * @date 18/11/5
 */
public class PacketEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        PacketCodeC.INSTANCE.encode(out, msg);
    }
}
