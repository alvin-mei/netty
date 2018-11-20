package io.netty.example.ex1.utils;

import io.netty.channel.Channel;
import io.netty.example.ex1.constatnts.Attributes;
import io.netty.util.Attribute;

/**
 * @author meijingling
 * @date 18/11/5
 */
public class LoginUtil {

    public static void markAsLogin(Channel channel) {
        channel.attr(Attributes.LOGIN).set(true);
        channel.config();
    }

    public static boolean hashLogin(Channel channel) {
        Attribute<Boolean> loginAttr = channel.attr(Attributes.LOGIN);

        return loginAttr.get() != null;
    }
}
