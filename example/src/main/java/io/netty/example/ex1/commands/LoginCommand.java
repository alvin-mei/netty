package io.netty.example.ex1.commands;

import io.netty.channel.Channel;
import io.netty.example.ex1.packets.LoginRequestPacket;

import java.util.Scanner;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class LoginCommand implements ConsoleCommand {
    LoginRequestPacket loginRequestPacket = new LoginRequestPacket();

    @Override
    public void exec(Scanner scanner, Channel channel) {
        System.out.println("请输入用户名登录：");
        String userName = scanner.nextLine();
        loginRequestPacket.setUserName(userName);
        loginRequestPacket.setPassword("pwd");
        channel.writeAndFlush(loginRequestPacket);
        _waitForLoginResponse();
    }

    private static void _waitForLoginResponse() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }
}
