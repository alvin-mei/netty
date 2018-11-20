package io.netty.example.echo.reactor;

import io.netty.example.echo.common.CodecUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author meijingling
 * @date 18/11/16
 */
public class SubReactor {
    private Selector subSelector;
    private ConcurrentLinkedQueue<SocketChannel> taskQueue;

    public SubReactor() {
        try {
            subSelector = SelectorProvider.provider().openSelector();
        } catch (IOException e) {
            System.out.println("初始化sub Reactor失败");
            e.printStackTrace();
        }
        taskQueue = new ConcurrentLinkedQueue<SocketChannel>();

        new Thread(() -> {
            while (true) {

                int count = 0;
                try {
                    count = subSelector.select(500);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                register();

                if (count <= 0) {
                    continue;
                }

                Set<SelectionKey> keys = subSelector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()) {
                        try {
                            _handleReadableKey(key);
                        } catch (ClosedChannelException e) {
                            e.printStackTrace();
                        }
                    }
                    if (key.isWritable()) {
                        try {
                            _handleWriteableKey(key);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void registerChannel(SocketChannel channel) {
        taskQueue.offer(channel);
        this.subSelector.wakeup();
    }

    private void _handleReadableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        // 没有数据则表明链接已经断开
        if (null == readBuffer) {
            System.out.println("链接已经断开，释放channel");
            clientSocketChannel.register(subSelector, 0);
            return;
        }

        // 有数据则打印数据
        if (readBuffer.position() > 0) {
            String content = CodecUtil.newString(readBuffer);
            System.out.println("get content:" + content);

            List<String> responseQueue = (List<String>) key.attachment();
            System.out.println("get attachment !");
            responseQueue.add("ping");
            System.out.println("add ping success");
            if ("ping".equals(content)) {
                System.out.println(Thread.currentThread().getName() + " -> get message:" + content);
                clientSocketChannel.register(subSelector, SelectionKey.OP_WRITE, key.attachment());
            }
        }
    }

    // 处理写事件
    private void _handleWriteableKey(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        List<String> queue = (List<String>) key.attachment();
        for (String str : queue) {
            System.out.println("server write:" + str);
            CodecUtil.write(clientSocketChannel, str);
        }

        queue.clear();
        clientSocketChannel.register(subSelector, SelectionKey.OP_READ, queue);

    }

    private void register() {
        if (taskQueue.isEmpty()) {
            return;
        }
        SocketChannel socketChannel = null;
        while (null != (socketChannel = taskQueue.poll())) {
            try {
                socketChannel.register(subSelector, SelectionKey.OP_READ, new ArrayList<>());
            } catch (ClosedChannelException e) {
                e.printStackTrace();
            }
        }
    }

}
