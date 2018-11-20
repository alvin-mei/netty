package io.netty.example.ex1.packets;

import lombok.Data;

import static io.netty.example.ex1.constatnts.Command.JOIN_GROUP_REQUEST;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class JoinGroupRequestPacket extends Packet {

    private String groupId;

    @Override
    public Byte getCommand() {
        return JOIN_GROUP_REQUEST;
    }
}
