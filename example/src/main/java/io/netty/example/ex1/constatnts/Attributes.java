package io.netty.example.ex1.constatnts;

import io.netty.example.ex1.Session;
import io.netty.util.AttributeKey;

/**
 * @author meijingling
 * @date 18/11/5
 */
public interface Attributes {
    AttributeKey<Boolean> LOGIN = AttributeKey.newInstance("login");
    AttributeKey<Session> SESSION = AttributeKey.newInstance("session");
}
