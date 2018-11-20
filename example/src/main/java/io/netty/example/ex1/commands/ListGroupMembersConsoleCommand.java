package io.netty.example.ex1.commands;

import io.netty.channel.Channel;
import io.netty.example.ex1.packets.ListGroupMembersRequestPacket;

import java.util.Scanner;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class ListGroupMembersConsoleCommand implements ConsoleCommand{

    @Override
    public void exec(Scanner scanner, Channel channel) {
        ListGroupMembersRequestPacket listGroupMembersRequestPacket = new ListGroupMembersRequestPacket();

        System.out.print("输入 groupId，获取群成员列表：");
        String groupId = scanner.next();

        listGroupMembersRequestPacket.setGroupId(groupId);
        channel.writeAndFlush(listGroupMembersRequestPacket);
    }
}
