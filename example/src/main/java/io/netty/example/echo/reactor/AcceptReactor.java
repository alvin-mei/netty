package io.netty.example.echo.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author meijingling
 * @date 18/11/19
 */
public class AcceptReactor implements Runnable {
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private SubReactorGroup subReactorGroup;

    public AcceptReactor(ServerSocketChannel channel) throws IOException {
        this.serverSocketChannel = channel;
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        subReactorGroup = new SubReactorGroup();
    }

    @Override
    public void run() {
        try {
            while (true) {

                    int count = selector.select(500);
                    if (count <= 0) {
                        continue;
                    }
                    System.out.println("main reactor get accept channel:" + count);

                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
                            clientSocketChannel.configureBlocking(false);
                            // log
                            System.out.println(Thread.currentThread().getName() + ": 接受新的channel");
                            SubReactor subReactor = subReactorGroup.next();
                            subReactor.registerChannel(clientSocketChannel);
                        }

                    }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
