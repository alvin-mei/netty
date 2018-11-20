package io.netty.example.ex1.constatnts;

import io.netty.example.ex1.utils.JsonSerializer;

/**
 * @author meijingling
 * @date 18/11/2
 */
public interface Serializer {

    byte JSON_SERIALIZER = 1;

    Serializer DEFAULT = new JsonSerializer();

    byte getSerializerAlgorithm();
    byte[] serializer(Object object);
    <T> T deserialize(Class<T> clazz, byte[] bytes);
}
