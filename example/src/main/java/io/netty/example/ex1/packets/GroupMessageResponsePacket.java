package io.netty.example.ex1.packets;

import lombok.Data;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class GroupMessageResponsePacket extends Packet {
    private String fromGroupId;
    private String message;
    private String fromUserId;

    @Override
    public Byte getCommand() {
        return null;
    }
}
