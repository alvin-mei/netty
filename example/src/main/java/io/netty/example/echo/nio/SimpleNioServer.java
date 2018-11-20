package io.netty.example.echo.nio;


import io.netty.example.echo.common.CodecUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author meijingling
 * @date 18/10/17
 */
public class SimpleNioServer {

    private ServerSocketChannel serverSocketChannel;
    private Selector bossSelector;

    public SimpleNioServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));

        bossSelector = Selector.open();

        serverSocketChannel.register(bossSelector, SelectionKey.OP_ACCEPT);

        _handleKeys();
    }

    private void _handleKeys() throws IOException {
        while (true) {
            //1. 阻塞等待事件
            int selectNums = bossSelector.select(30 * 1000L);
            if (0 == selectNums) {
                continue;
            }

            //2. 事件列表
            Iterator<SelectionKey> iterator = bossSelector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                    continue;
                }
                //3. 分发事件
                _dispatch(key);
            }
        }
    }

    private void _dispatch(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            _handleAcceptableKey(key);
        }

        if (key.isReadable()) {
            _handleReadableKey(key);
        }
        if (key.isWritable()) {
            _handleWriteableKey(key);
        }
    }

    private void _handleAcceptableKey(SelectionKey key) throws IOException {
        SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
        clientSocketChannel.configureBlocking(false);
        // log
        System.out.println("接受新的channel");
        clientSocketChannel.register(bossSelector, SelectionKey.OP_READ, new ArrayList<String>());
    }

    private void _handleReadableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();
        ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
        // 没有数据则表明链接已经断开
        if (null == readBuffer) {
            System.out.println("链接已经断开，释放channel");
            clientSocketChannel.register(bossSelector, 0);
            return;
        }

        // 有数据则打印数据
        if (readBuffer.position() > 0) {
            String content = CodecUtil.newString(readBuffer);
            System.out.println("读取数据:" + content);

            // 添加到响应队列
            List<String> responseQueue = (List<String>) key.attachment();
            responseQueue.add(content);
            clientSocketChannel.register(bossSelector, SelectionKey.OP_WRITE, key.attachment());
        }
    }

    // 处理写事件
    private void _handleWriteableKey(SelectionKey key) throws ClosedChannelException {
        SocketChannel clientSocketChannel = (SocketChannel) key.channel();

        List<String> responseQueue = (List<String>) key.attachment();
        for (String content : responseQueue) {
            System.out.println("写入数据:" + content);
            CodecUtil.write(clientSocketChannel, content);
        }
        responseQueue.clear();

        clientSocketChannel.register(bossSelector, SelectionKey.OP_READ, responseQueue);

    }

    public static void main(String[] args) throws IOException {
        SimpleNioServer server = new SimpleNioServer();
    }
}
