package io.netty.example.ex1.packets;

import io.netty.example.ex1.Session;
import lombok.Data;

import java.util.List;

import static io.netty.example.ex1.constatnts.Command.LIST_GROUP_MEMBERS_RESPONSE;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class ListGroupMembersResponsePacket extends Packet {
    private String groupId;
    private List<Session> sessions;
    @Override
    public Byte getCommand() {
        return LIST_GROUP_MEMBERS_RESPONSE;
    }
}
