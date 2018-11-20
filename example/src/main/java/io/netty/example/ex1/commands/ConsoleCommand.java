package io.netty.example.ex1.commands;

import io.netty.channel.Channel;

import java.util.Scanner;

/**
 * @author meijingling
 * @date 18/11/6
 */
public interface ConsoleCommand {
    void exec(Scanner scanner, Channel channel);
}
