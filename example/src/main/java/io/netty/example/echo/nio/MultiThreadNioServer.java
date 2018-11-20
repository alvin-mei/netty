package io.netty.example.echo.nio;

import io.netty.example.echo.common.CodecUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author meijingling
 * @date 18/11/16
 */
public class MultiThreadNioServer {
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    MultiThreadNioServer() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.socket().bind(new InetSocketAddress(8080));

        selector = Selector.open();

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

       while(true) {
            int num = selector.select();
            if(num <= 0) {
                continue;
            }

           Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while(iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                if(key.isAcceptable()) {
                    SocketChannel clientSocketChannel = ((ServerSocketChannel) key.channel()).accept();
                    clientSocketChannel.configureBlocking(false);
                    // log
                    System.out.println(Thread.currentThread().getName() + ": 接受新的channel");
                    SelectionKey ioKey = clientSocketChannel.register(selector, SelectionKey.OP_READ, new ArrayList<String>());
                    ioKey.attach(new IOProcess());
                } else {
                    IOProcess ioProcess = (IOProcess) key.attachment();
                    ioProcess.process(key);
                }
            }
       }
    }

    class IOProcess {

        public void process(SelectionKey key) {
            executorService.submit(() -> {
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
                    } catch (ClosedChannelException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private void _handleReadableKey(SelectionKey key) throws ClosedChannelException {
            SocketChannel clientSocketChannel = (SocketChannel) key.channel();
            ByteBuffer readBuffer = CodecUtil.read(clientSocketChannel);
            // 没有数据则表明链接已经断开
            if (null == readBuffer) {
                System.out.println("链接已经断开，释放channel");
                clientSocketChannel.register(selector, 0);
                return;
            }

            // 有数据则打印数据
            if (readBuffer.position() > 0) {
                String content = CodecUtil.newString(readBuffer);
                System.out.println("server thread:" + Thread.currentThread().getName()  + " ---> 读取数据:" + content);

                // 添加到响应队列
                List<String> responseQueue = (List<String>) key.attachment();
                responseQueue.add(content);
                clientSocketChannel.register(selector, SelectionKey.OP_WRITE, key.attachment());
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

            clientSocketChannel.register(selector, SelectionKey.OP_READ, responseQueue);

        }
    }

    public static void main(String[] args) throws IOException {
        MultiThreadNioServer server = new MultiThreadNioServer();

    }

}
