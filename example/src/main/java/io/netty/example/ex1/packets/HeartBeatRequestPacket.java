package io.netty.example.ex1.packets;

import static io.netty.example.ex1.constatnts.Command.HEART_BEAT_REQUEST;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class HeartBeatRequestPacket extends Packet{
    @Override
    public Byte getCommand() {
        return HEART_BEAT_REQUEST;
    }
}
