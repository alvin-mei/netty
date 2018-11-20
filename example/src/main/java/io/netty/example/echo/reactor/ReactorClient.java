package io.netty.example.echo.reactor;


import io.netty.example.echo.common.CodecUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

/**
 * @author meijingling
 * @date 18/10/17
 */
public class ReactorClient {
    private SocketChannel clientSocketChannel;
    private Selector selector;

    private CountDownLatch connected = new CountDownLatch(1);

    public ReactorClient() throws IOException, InterruptedException {
        clientSocketChannel = SocketChannel.open();
        clientSocketChannel.configureBlocking(false);

        selector = Selector.open();
        // 注册 Server Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
        clientSocketChannel.connect(new InetSocketAddress(8080));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handleKeys();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if (connected.getCount() != 0) {
            connected.await();
        }
        System.out.println("client 启动完成");
    }

    private void handleKeys() throws IOException {
        while (true) {
            // 通过selector 选择 channel
            int selectNums = selector.select(30 * 1000L);
            if (0 == selectNums) {
                continue;
            }

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                    continue;
                }

                handleKey(key);
            }
        }
    }

    private synchronized void handleKey(SelectionKey key) throws IOException {
        // 接受连接就绪
        if (key.isConnectable()) {
            handleConnectableKey(key);
        }
        // 读就绪
        if (key.isReadable()) {
            handleReadableKey(key);
        }
        // 写就绪
        if (key.isWritable()) {
            handleWritableKey(key);
        }
    }

    private void handleConnectableKey(SelectionKey key) throws IOException {
        // 完成连接
        if (!clientSocketChannel.isConnectionPending()) {
            return;
        }
        clientSocketChannel.finishConnect();
        // log
        System.out.println("接受新的 Channel");
        // 注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
        // 标记为已连接
        connected.countDown();
    }

    @SuppressWarnings("Duplicates")
    private void handleReadableKey(SelectionKey key) throws ClosedChannelException {
        // Client Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        // 读取数据
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        // 打印数据
        if (readBuffer.position() > 0) { // 写入模式下，
            String content = CodecUtil.newString(readBuffer);
            System.out.println("读取数据：" + content);
        }
        clientSocketChannel.register(selector, SelectionKey.OP_READ);

    }

    @SuppressWarnings("Duplicates")
    private void handleWritableKey(SelectionKey key) throws ClosedChannelException {
        // Client Socket Channel
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        System.out.println("write ping");
        // 注册 Client Socket Channel 到 Selector
        clientSocketChannel.register(selector, SelectionKey.OP_READ);
    }

    public synchronized void beat() throws ClosedChannelException {
        CodecUtil.write(clientSocketChannel, "ping");
        // 注册 Client Socket Channel 到 Selector
        System.out.println("ping");
        clientSocketChannel.register(selector, SelectionKey.OP_WRITE, MessageEvent.BEAT);
        selector.wakeup();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 0; i <= 1; i++) {
            new Thread(() -> {
                ReactorClient client = null;
                try {
                    client = new ReactorClient();
                    for (int j = 0; j < 30; j++) {
                        client.beat();
                        Thread.sleep(1000L);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }).start();
        }
    }

}


