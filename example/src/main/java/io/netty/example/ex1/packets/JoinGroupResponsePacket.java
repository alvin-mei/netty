package io.netty.example.ex1.packets;

import lombok.Data;

import static io.netty.example.ex1.constatnts.Command.JOIN_GROUP_RESPONSE;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class JoinGroupResponsePacket extends Packet {
    private boolean success;
    private String groupId;
    private String reason;

    @Override
    public Byte getCommand() {
        return JOIN_GROUP_RESPONSE;
    }
}
