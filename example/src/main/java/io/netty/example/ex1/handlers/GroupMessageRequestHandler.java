package io.netty.example.ex1.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.example.ex1.packets.GroupMessageRequestPacket;
import io.netty.example.ex1.packets.GroupMessageResponsePacket;
import io.netty.example.ex1.utils.SessionUtil;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class GroupMessageRequestHandler extends SimpleChannelInboundHandler<GroupMessageRequestPacket> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, GroupMessageRequestPacket groupMessageRequestPacket) throws Exception {
            String groupId = groupMessageRequestPacket.getGroupId();
        ChannelGroup channelGroup = SessionUtil.getChannelGroup(groupId);

        GroupMessageResponsePacket groupMessageResponsePacket = new GroupMessageResponsePacket();
        groupMessageResponsePacket.setFromGroupId(groupId);
        groupMessageResponsePacket.setFromUserId(SessionUtil.getSession(ctx.channel()).getUserId());
        if(null != channelGroup && !channelGroup.isEmpty()) {
            channelGroup.writeAndFlush(groupMessageResponsePacket);
        }
    }
}
