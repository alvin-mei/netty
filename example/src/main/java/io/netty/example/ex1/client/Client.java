package io.netty.example.ex1.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.example.ex1.commands.LoginCommand;
import io.netty.example.ex1.commands.ConsoleCommandManager;
import io.netty.example.ex1.handlers.*;
import io.netty.example.ex1.utils.SessionUtil;
import io.netty.util.AttributeKey;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author meijingling
 * @date 18/11/1
 */
public class Client {
    private final static Integer MAX_RETRY_COUNT = 5;

    public static void main(String[] args) {
        NioEventLoopGroup work = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(work)
                .channel(NioSocketChannel.class)
                .attr(AttributeKey.newInstance("clientName"), "nettyClient")
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(PacketCodecHandler.INSTANCE);
                        ch.pipeline().addLast(new LoginResponseHandler());
                        ch.pipeline().addLast(new ListGroupMembersResponseHandler());
                        ch.pipeline().addLast(new HeartBeatTimerHandler());
                        ch.pipeline().addLast(new CreateGroupResponseHandler());
                        ch.pipeline().addLast(new MessageResponseHandler());
                    }
                });

        _connect(bootstrap, "127.0.0.1", 8000, 1);

    }

    private static void _connect(final Bootstrap bootstrap,
                                 final String host,
                                 final int port,
                                 final int retry) {
        bootstrap.connect(host, port).addListener(future -> {
            if (future.isSuccess()) {
                System.out.println("客户端连接成功！");
                Channel channel = ((ChannelFuture) future).channel();
                startConsoleThread(channel);
            } else {
                if (retry <= MAX_RETRY_COUNT) {
                    System.out.println("尝试重新连接第 " + retry + "次！");
                    bootstrap.config().group().schedule(() -> _connect(bootstrap, host, port, retry + 1), (retry + 1) << 1, TimeUnit.SECONDS);
                } else {
                    System.out.println("达到最大重试次数 " + MAX_RETRY_COUNT + "!");
                }
            }
        });
    }

    private static void startConsoleThread(Channel channel) {
        ConsoleCommandManager consoleCommandManager = new ConsoleCommandManager();
        LoginCommand loginCommand = new LoginCommand();
        Scanner scanner = new Scanner(System.in);

        new Thread(() -> {
            while (!Thread.interrupted()) {
                if (!SessionUtil.hasLogin(channel)) {
                    loginCommand.exec(scanner, channel);
                } else {
                    consoleCommandManager.exec(scanner, channel);
                }
            }
        }).start();
    }




}
