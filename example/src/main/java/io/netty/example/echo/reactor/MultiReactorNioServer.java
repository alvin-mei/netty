package io.netty.example.echo.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

/**
 * @author meijingling
 * @date 18/11/16
 */
public class MultiReactorNioServer {

    private ServerSocketChannel serverSocketChannel;

    MultiReactorNioServer(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(port));

        new Thread(new AcceptReactor(serverSocketChannel)).start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        MultiReactorNioServer server = new MultiReactorNioServer(8080);
    }
}
