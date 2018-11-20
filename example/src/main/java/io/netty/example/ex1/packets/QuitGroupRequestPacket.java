package io.netty.example.ex1.packets;

import lombok.Data;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class QuitGroupRequestPacket extends Packet {
    private String groupId;

    @Override
    public Byte getCommand() {
        return null;
    }
}
