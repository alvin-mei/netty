package io.netty.example.echo.common;


import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * @author meijingling
 * @date 18/10/17
 */
public class CodecUtil {
    public static ByteBuffer read(SocketChannel channel){
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        try {
            int count = channel.read(byteBuffer);
            if(-1 == count) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteBuffer;
    }

    public static void write(SocketChannel channel, String content) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try {
            buffer.put(content.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        buffer.flip();

        try {
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String newString(ByteBuffer buffer) {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        System.arraycopy(buffer.array(), buffer.position(), bytes, 0, buffer.remaining());
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
