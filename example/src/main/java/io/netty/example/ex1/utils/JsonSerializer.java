package io.netty.example.ex1.utils;

import com.alibaba.fastjson.JSON;
import io.netty.example.ex1.constatnts.Serializer;

/**
 * @author meijingling
 * @date 18/11/2
 */
public class JsonSerializer implements Serializer {

    @Override
    public byte getSerializerAlgorithm() {
        return Serializer.JSON_SERIALIZER;
    }

    @Override
    public byte[] serializer(Object object) {
        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(bytes, clazz);
    }
}
