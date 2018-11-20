package io.netty.example.echo.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author meijingling
 * @date 18/11/14
 */
public class BioEchoClient implements Runnable {
    private String serverAddress;
    private int serverPort;
    private Socket client = null;
    private PrintWriter printWriter = null;
    private BufferedReader bufferedReader = null;
    private int taskId;

    private static ExecutorService executorService = Executors.newFixedThreadPool(64);

    BioEchoClient(String serverAddress, int serverPort, int taskId) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.taskId = taskId;
    }

    @Override
    public void run() {
        try {
            client = new Socket();
            client.connect(new InetSocketAddress(serverAddress, serverPort));
            printWriter = new PrintWriter(client.getOutputStream(), true);
            for (int i = 1; i <= 10; i++) {
                printWriter.println("client(" + taskId + ") ---> 第" + i + "次发送 hello!");
                printWriter.flush();
            }

            bufferedReader = new BufferedReader(new InputStreamReader(client.getInputStream()));            //读取服务器返回的信息并进行输出
            while (null != bufferedReader.readLine()) {
                System.out.println("client(" + taskId + ") 来自服务器的信息是：" + bufferedReader.readLine());
            }
        } catch (SocketException e) {
            System.out.println("server 断开了连接!");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println("结束");
            printWriter.close();
            try {
                bufferedReader.close();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        for (int i = 0; i <= 3; i++) {
            executorService.submit(new BioEchoClient("localhost", 8899, i));
        }
    }
}
