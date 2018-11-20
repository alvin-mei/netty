package io.netty.example.ex1.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.example.ex1.packets.JoinGroupRequestPacket;
import io.netty.example.ex1.packets.JoinGroupResponsePacket;
import io.netty.example.ex1.utils.SessionUtil;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class JoinGroupHandler extends SimpleChannelInboundHandler<JoinGroupRequestPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JoinGroupRequestPacket joinGroupRequestPacket) throws Exception {
        // 1. 获取群对应的 channelGroup，然后将当前用户的 channel 添加进去
        String groupId = joinGroupRequestPacket.getGroupId();
        ChannelGroup channelGroup = SessionUtil.getChannelGroup(groupId);

        JoinGroupResponsePacket responsePacket = new JoinGroupResponsePacket();
        responsePacket.setGroupId(groupId);

        if(channelGroup!= null) {
            channelGroup.add(ctx.channel());
            responsePacket.setSuccess(true);
        } else {
            responsePacket.setSuccess(false);
            responsePacket.setReason("该群组不存在!");
        }

        ctx.channel().writeAndFlush(responsePacket);

    }
}
