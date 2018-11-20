package io.netty.example.ex1.utils;

import java.util.UUID;

/**
 * @author meijingling
 * @date 18/11/6
 */
public class IdGeneratorUtil {

    public static String getId() {
        return UUID.randomUUID().toString().split("-")[0];
    }
}
