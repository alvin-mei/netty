package io.netty.example.ex1.commands;

import io.netty.channel.Channel;
import io.netty.example.ex1.packets.CreateGroupRequestPacket;

import java.util.Arrays;
import java.util.Scanner;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class CreateGroupConsoleCommand implements ConsoleCommand{
    private static final String USER_ID_SPLITER = ",";

    @Override
    public void exec(Scanner scanner, Channel channel) {
        CreateGroupRequestPacket createGroupRequestPacket = new CreateGroupRequestPacket();

        System.out.print("【拉人群聊】输入 userId 列表，userId 之间英文逗号隔开：");
        String userIds = scanner.next();

        createGroupRequestPacket.setUserIds(Arrays.asList(userIds.split(USER_ID_SPLITER)));
        channel.writeAndFlush(createGroupRequestPacket);
    }
}
