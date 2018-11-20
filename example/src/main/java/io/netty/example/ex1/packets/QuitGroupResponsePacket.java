package io.netty.example.ex1.packets;

import lombok.Data;

import java.util.List;

/**
 * @author meijingling
 * @date 18/11/6
 */
@Data
public class QuitGroupResponsePacket extends Packet{
    private boolean success;
    private String groupId;
    private List<String> userNames;

    @Override
    public Byte getCommand() {
        return null;
    }
}
