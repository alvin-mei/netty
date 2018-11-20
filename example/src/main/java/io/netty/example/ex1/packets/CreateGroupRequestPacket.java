package io.netty.example.ex1.packets;

import io.netty.example.ex1.utils.PacketCodeC;
import lombok.Data;

import java.util.List;

import static io.netty.example.ex1.constatnts.Command.CREATE_GROUP_REQUEST;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class CreateGroupRequestPacket extends Packet{
    private List<String> userIds;

    @Override
    public Byte getCommand() {
        return CREATE_GROUP_REQUEST;
    }
}
