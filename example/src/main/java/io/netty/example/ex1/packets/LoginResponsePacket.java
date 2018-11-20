package io.netty.example.ex1.packets;

import lombok.Data;

import static io.netty.example.ex1.constatnts.Command.LOGIN_RESPONSE;

/**
 * @author meijingling
 * @date 18/11/4
 */
@Data
public class LoginResponsePacket extends Packet {
    private String userId;

    private String userName;

    private boolean success;

    private String reason;

    @Override
    public Byte getCommand() {
        return LOGIN_RESPONSE;
    }
}
