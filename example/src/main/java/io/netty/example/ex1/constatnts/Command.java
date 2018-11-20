package io.netty.example.ex1.constatnts;

/**
 * @author meijingling
 * @date 18/11/4
 */
public interface Command {
    Byte LOGIN_REQUEST = 1;
    Byte LOGIN_RESPONSE = 2;
    Byte MESSAGE_REQUEST = 3;
    Byte MESSAGE_RESPONSE = 4;
    Byte CREATE_GROUP_REQUEST = 5;
    Byte CREATE_GROUP_RESPONSE = 6;
    Byte JOIN_GROUP_REQUEST = 7;
    Byte JOIN_GROUP_RESPONSE = 8;
    Byte LIST_GROUP_MEMBERS_REQUEST = 9;
    Byte LIST_GROUP_MEMBERS_RESPONSE = 10;
    Byte SEND_GROUP_MESSAGE_REQUEST = 11;
    Byte SEND_GROUP_MESSAGE_RESPONSE = 12;
    Byte HEART_BEAT_REQUEST = 13;
    Byte HEART_BEAT_RESPONSE = 14;
}
