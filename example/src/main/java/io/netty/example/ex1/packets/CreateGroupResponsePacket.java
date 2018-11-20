package io.netty.example.ex1.packets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static io.netty.example.ex1.constatnts.Command.CREATE_GROUP_RESPONSE;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupResponsePacket extends Packet{
    private boolean success;
    private String groupId;
    private List<String> userNames;

    @Override
    public Byte getCommand() {
        return CREATE_GROUP_RESPONSE;
    }
}
