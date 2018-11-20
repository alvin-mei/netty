//package io.netty.example.ex1.server;
//
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.channel.ChannelInboundHandlerAdapter;
//import io.netty.example.ex1.packets.Packet;
//import io.netty.example.ex1.packets.LoginRequestPacket;
//import io.netty.example.ex1.packets.LoginResponsePacket;
//import io.netty.example.ex1.packets.MessageRequestPacket;
//import io.netty.example.ex1.packets.MessageResponsePacket;
//import io.netty.example.ex1.utils.LoginUtil;
//import io.netty.example.ex1.utils.PacketCodeC;
//
//import java.util.Date;
//
///**
// * @author meijingling
// * @date 18/11/4
// */
//public class ServerHandler extends ChannelInboundHandlerAdapter {
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        ByteBuf byteBuf = (ByteBuf) msg;
//
//        Packet packet = PacketCodeC.INSTANCE.decode(byteBuf);
//
//        if (packet instanceof LoginRequestPacket) {
//            LoginRequestPacket loginRequestPacket = (LoginRequestPacket) packet;
//
//            LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
//
////            if (_valid(loginRequestPacket)) {
////                loginResponsePacket.setSuccess(true);
////                System.out.println("客户端登录成功！用户名：" + loginRequestPacket.getUserName());
////            } else {
////                loginResponsePacket.setSuccess(false);
////                loginResponsePacket.setReason("用户名密码错误！");
////                System.out.println("客户端登录失败！用户名：" + loginRequestPacket.getUserName());
////            }
//            ByteBuf responseByteBuf = PacketCodeC.INSTANCE.encode(loginResponsePacket);
//            ctx.channel().writeAndFlush(responseByteBuf);
//        } else if(packet instanceof MessageRequestPacket) {
//            MessageRequestPacket messageRequestPacket = (MessageRequestPacket) packet;
//            System.out.println(new Date() + "：收到客户端消息：" + messageRequestPacket.getMessage());
//            MessageResponsePacket messageResponsePacket = new MessageResponsePacket();
//
//            messageResponsePacket.setMessage("服务端回复【" + messageRequestPacket.getMessage() + "】");
//            ByteBuf responseByteBuf = PacketCodeC.INSTANCE.encode(messageResponsePacket);
//
//            ctx.channel().writeAndFlush(responseByteBuf);
//        }
//
//    }
//
//    private boolean _valid(LoginRequestPacket loginRequestPacket) {
//        return true;
//    }
//
//
//}
