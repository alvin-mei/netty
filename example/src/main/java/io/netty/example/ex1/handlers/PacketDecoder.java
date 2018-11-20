package io.netty.example.ex1.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.example.ex1.utils.PacketCodeC;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author meijingling
 * @date 18/11/5
 */
public class PacketDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(PacketCodeC.INSTANCE.decode(in));
    }
}
