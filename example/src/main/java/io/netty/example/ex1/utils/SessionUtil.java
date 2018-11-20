package io.netty.example.ex1.utils;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.example.ex1.Session;
import io.netty.example.ex1.constatnts.Attributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author meijingling
 * @date 18/11/5
 */
public class SessionUtil {
    private static Map<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    private static final Map<String, ChannelGroup> GROUP_ID_MAP = new ConcurrentHashMap<>();


    public static void bindSession(Session session, Channel channel) {
        USER_CHANNEL_MAP.put(session.getUserId(), channel);
        channel.attr(Attributes.SESSION).set(session);
    }

    public static void unBindSession(Channel channel) {
        if(hasLogin(channel)) {
            USER_CHANNEL_MAP.remove(getSession(channel).getUserId());
            channel.attr(Attributes.SESSION).set(null);
        }

    }

    public static boolean hasLogin(Channel channel) {

        return channel.hasAttr(Attributes.SESSION);
    }

    public static Session getSession(Channel channel) {
        return channel.attr(Attributes.SESSION).get();
    }

    public static Channel getChannel(String userId) {
        return USER_CHANNEL_MAP.get(userId);
    }

    public static void bindChannelGroup(String groupId, ChannelGroup channelGroup) {
        GROUP_ID_MAP.put(groupId, channelGroup);
    }

    public static ChannelGroup getChannelGroup(String groupId) {
        return GROUP_ID_MAP.get(groupId);
    }
}
