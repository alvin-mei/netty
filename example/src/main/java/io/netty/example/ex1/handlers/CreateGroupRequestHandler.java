package io.netty.example.ex1.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.example.ex1.packets.CreateGroupRequestPacket;
import io.netty.example.ex1.packets.CreateGroupResponsePacket;
import io.netty.example.ex1.utils.IdGeneratorUtil;
import io.netty.example.ex1.utils.SessionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class CreateGroupRequestHandler extends SimpleChannelInboundHandler<CreateGroupRequestPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CreateGroupRequestPacket createGroupRequestPacket) throws Exception {
        List<String> userIds =  createGroupRequestPacket.getUserIds();

        List<String> userNames = new ArrayList<>();
        // 创建一个channel 分组
        ChannelGroup channelGroup = new DefaultChannelGroup(ctx.executor());

        // 筛选出需要加入群聊的用户和userName
        for(String userId: userIds) {
            Channel channel = SessionUtil.getChannel(userId);
            if(null != channel) {
                channelGroup.add(channel);
                userNames.add(SessionUtil.getSession(channel).getUserName());
            }
        }
        // 创建群聊创建结果响应
        CreateGroupResponsePacket createGroupResponsePacket = new CreateGroupResponsePacket();
        createGroupResponsePacket.setSuccess(true);
        createGroupResponsePacket.setGroupId(IdGeneratorUtil.getId());
        createGroupResponsePacket.setUserNames(userNames);

        SessionUtil.bindChannelGroup(createGroupResponsePacket.getGroupId(), channelGroup);

        channelGroup.writeAndFlush(createGroupResponsePacket);
        System.out.print("群创建成功，id 为[" + createGroupResponsePacket.getGroupId() + "], ");
        System.out.println("群里面有：" + createGroupResponsePacket.getUserNames());
    }
}
