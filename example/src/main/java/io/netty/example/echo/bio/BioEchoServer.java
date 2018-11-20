package io.netty.example.echo.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author meijingling
 * @date 18/11/14
 */
public class BioEchoServer {
    private static ExecutorService executorService = Executors.newFixedThreadPool(16);

    BioEchoServer(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(8899);
            System.out.println("bio echo server 启动成功!");
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println(client.getRemoteSocketAddress() + " 客户端连接成功!");
                executorService.submit(new BioMsgHandle(client));
            }
        } catch (Exception e) {
            System.out.println("bio echo server 启动失败！");
            e.printStackTrace();
        }

    }

    private static class BioMsgHandle implements Runnable {
        Socket client;

        public BioMsgHandle(Socket socket) {
            this.client = socket;
        }

        @Override
        public void run() {
            BufferedReader bufferedReader = null;
            PrintWriter printWriter = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                printWriter = new PrintWriter(client.getOutputStream(), true);
                String inputLine = null;
                while (null != (inputLine = bufferedReader.readLine())) {
                    printWriter.println(inputLine);
                    printWriter.flush();
                    System.out.println("从client 获取消息:" + inputLine);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bufferedReader.close();
                    printWriter.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void main(String[] args) throws IOException {
        BioEchoServer server = new BioEchoServer(8899);
    }

}
