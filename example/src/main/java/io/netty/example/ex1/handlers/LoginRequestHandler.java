package io.netty.example.ex1.handlers;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.example.ex1.Session;
import io.netty.example.ex1.packets.LoginRequestPacket;
import io.netty.example.ex1.packets.LoginResponsePacket;
import io.netty.example.ex1.utils.IdGeneratorUtil;
import io.netty.example.ex1.utils.LoginUtil;
import io.netty.example.ex1.utils.SessionUtil;

import java.util.Date;
import java.util.UUID;

/**
 * @author meijingling
 * @date 18/11/5
 */
@ChannelHandler.Sharable
public class LoginRequestHandler extends SimpleChannelInboundHandler<LoginRequestPacket> {

    public static final LoginRequestHandler INSTANCE = new LoginRequestHandler();

    protected LoginRequestHandler() {
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, LoginRequestPacket loginRequestPacket) throws Exception {
        System.out.println(new Date() + ": 收到客户端登录请求……");

        LoginResponsePacket loginResponsePacket = _login(ctx, loginRequestPacket);
        ctx.channel().writeAndFlush(loginResponsePacket);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        SessionUtil.unBindSession(ctx.channel());
    }

    private LoginResponsePacket _login(ChannelHandlerContext ctx, LoginRequestPacket loginRequestPacket) {
        LoginResponsePacket loginResponsePacket = new LoginResponsePacket();
        loginResponsePacket.setVersion(loginRequestPacket.getVersion());
        loginResponsePacket.setUserName(loginRequestPacket.getUserName());

        if (_valid(loginRequestPacket)) {
            System.out.println("客户端登录成功！用户名：" + loginRequestPacket.getUserName());
            loginResponsePacket.setSuccess(true);
            String userId = IdGeneratorUtil.getId();
            loginResponsePacket.setUserId(userId);

            // 将session 与 channel绑定
            SessionUtil.bindSession(new Session(userId, loginRequestPacket.getUserName()), ctx.channel());

//            LoginUtil.markAsLogin(ctx.channel());
        } else {
            loginResponsePacket.setSuccess(false);
            loginResponsePacket.setReason("用户名密码错误！");
            System.out.println("客户端登录失败！用户名：" + loginRequestPacket.getUserName());
        }
        return loginResponsePacket;
    }


    private boolean _valid(LoginRequestPacket loginRequestPacket) {
        return true;
    }
}
