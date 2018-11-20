package io.netty.example.ex1.packets;

import lombok.Data;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class GroupMessageRequestPacket extends Packet{
    private String groupId;
    private String message;
    @Override
    public Byte getCommand() {
        return null;
    }
}
