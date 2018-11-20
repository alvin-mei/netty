//package io.netty.example.ex1.client;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.example.ex1.packets.MessageResponsePacket;
//import io.netty.example.ex1.packets.Packet;
//import io.netty.example.ex1.packets.LoginRequestPacket;
//import io.netty.example.ex1.packets.LoginResponsePacket;
//import io.netty.example.ex1.utils.LoginUtil;
//import io.netty.example.ex1.utils.PacketCodeC;
//
//import java.util.Date;
//import java.util.UUID;
//
///**
// * @author meijingling
// * @date 18/11/4
// */
//public class ClintHandler extends ChannelInboundHandlerAdapter {
//    @Override
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println(new Date() + "客户端开始发送登录请求");
//
//        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();
//        loginRequestPacket.setUserId(UUID.randomUUID().toString());
//        loginRequestPacket.setUserName("alvin");
//        loginRequestPacket.setPassword("pwd");
//
//        ByteBuf byteBuf = PacketCodeC.INSTANCE.encode(loginRequestPacket);
//
//        ctx.channel().writeAndFlush(byteBuf);
//    }
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        ByteBuf byteBuf = (ByteBuf) msg;
//
//        Packet packet = PacketCodeC.INSTANCE.decode(byteBuf);
//        if(packet instanceof LoginResponsePacket) {
//            LoginResponsePacket loginResponsePacket = (LoginResponsePacket) packet;
//            if(loginResponsePacket.isSuccess()) {
//                LoginUtil.markAsLogin(ctx.channel());
//                System.out.println("login success!");
//            } else {
//                System.out.println("login failed!");
//            }
//        } else if(packet instanceof MessageResponsePacket) {
//            MessageResponsePacket messageResponsePacket = (MessageResponsePacket) packet;
//            System.out.println(new Date() + ": 收到服务端的消息: " + messageResponsePacket.getMessage());
//        }
//    }
//}
