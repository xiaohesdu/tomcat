package com.gh.tomcat;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;

/**
 * @author gonghe.hogan
 */
@Slf4j
public class TomcatServer {

    /**
     * https://blog.csdn.net/qiangcai/article/details/60583330
     * tomcat原理解析(一)：一个简单的实现
     */
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
//            根据端口号启动一个serverSocket
            ServerSocket socket = new ServerSocket(PORT);
            ServerHandler serverHandler = new ServerHandler(socket);
            serverHandler.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ServerHandler extends Thread{
        ServerSocket serverSocket = null;
        public ServerHandler(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            while (true){
                try {
                    Socket client = null;
//                serverSocket阻塞等待客户端请求数据
                    client = serverSocket.accept();
                    if (client != null){
                        log.info("收到一个客户端的请求");
//                    根据客户端的Socket对象获取输入流对象
//                    封装字节流到字符流
                        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));


//                    HTTP请求由三部分组成，分别是请求行、消息报头（请求头）、请求正文
//                    1.第一行就是请求行。
                        String line = reader.readLine();
                        //line的格式如下：         GET /test.jpg /HTTP1.1
                        log.info("request line :{}",  line);
//                    1.1拆分http请求路径，获取http需求请求的完成路径
                        String resource = line.substring(line.indexOf('/'), line.lastIndexOf('/') - 5);
                        log.info("the resource you request is :{}", resource);
                        resource = URLDecoder.decode(resource, "UTF-8");

//                    1.2获取请求的方法类型，比如get、post类型
                        final StringTokenizer stringTokenizer = new StringTokenizer(line);
                        String method = stringTokenizer.nextElement().toString();
                        log.info("the request method you send is :{}", method);

//                     2. 继续循环读取浏览器客户端发出的一行一行的数据
                        while((line = reader.readLine()) != null){
                            if (line.isEmpty()){
//                          当line为空行的时候标志header消息结束
                                break;
                            }
                            log.info("the http header is {}", line);
                        }
                    }
                }catch (IOException e){
                    log.info("IO exception.");

                }

            }


        }
    }

}
