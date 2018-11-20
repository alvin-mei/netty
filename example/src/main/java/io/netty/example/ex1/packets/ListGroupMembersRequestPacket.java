package io.netty.example.ex1.packets;

import lombok.Data;

import static io.netty.example.ex1.constatnts.Command.LIST_GROUP_MEMBERS_REQUEST;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class ListGroupMembersRequestPacket extends Packet {
    private String groupId;

    @Override
    public Byte getCommand() {
        return LIST_GROUP_MEMBERS_REQUEST;
    }
}
