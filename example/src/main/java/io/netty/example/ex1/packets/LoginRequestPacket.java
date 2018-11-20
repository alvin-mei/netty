package io.netty.example.ex1.packets;

import lombok.Data;

/**
 * @author meijingling
 * @date 18/11/2
 */
@Data
public class LoginRequestPacket extends Packet {
    private String userId;
    private String userName;
    private String password;

    @Override
    public Byte getCommand() {
        return 1;
    }
}
