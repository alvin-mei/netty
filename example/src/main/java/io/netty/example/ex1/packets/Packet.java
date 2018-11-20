package io.netty.example.ex1.packets;

import lombok.Data;

/**
 * @author meijingling
 * @date 18/11/2
 */

@Data
public abstract class Packet {
    private Byte version = 1;
    public abstract Byte getCommand();
}
