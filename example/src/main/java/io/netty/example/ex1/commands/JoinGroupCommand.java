package io.netty.example.ex1.commands;

import io.netty.channel.Channel;
import io.netty.example.ex1.packets.JoinGroupRequestPacket;

import java.util.Scanner;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class JoinGroupCommand implements ConsoleCommand{

    @Override
    public void exec(Scanner scanner, Channel channel) {
        JoinGroupRequestPacket joinGroupRequsetPacket = new JoinGroupRequestPacket();

        System.out.println("请输入需要加入的group Id：");
        String groupId = scanner.nextLine();

        joinGroupRequsetPacket.setGroupId(groupId);
        channel.writeAndFlush(joinGroupRequsetPacket);

    }
}
