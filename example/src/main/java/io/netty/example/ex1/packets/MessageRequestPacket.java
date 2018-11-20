package io.netty.example.ex1.packets;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static io.netty.example.ex1.constatnts.Command.MESSAGE_REQUEST;

/**
 * @author meijingling
 * @date 18/11/4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequestPacket extends Packet {
    String toUserId;
    String message;

    @Override
    public Byte getCommand() {
        return MESSAGE_REQUEST;
    }
}
