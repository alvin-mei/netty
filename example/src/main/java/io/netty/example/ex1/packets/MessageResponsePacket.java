package io.netty.example.ex1.packets;

import lombok.Data;

import static io.netty.example.ex1.constatnts.Command.MESSAGE_RESPONSE;

/**
 * @author meijingling
 * @date 18/11/4
 */
@Data
public class MessageResponsePacket extends Packet {
    String fromUserId;
    String fromUserName;
    String message;

    @Override
    public Byte getCommand() {
        return MESSAGE_RESPONSE;
    }
}
