package com.gh.tomcat;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Objects;
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
            while (true) {
                try {
                    Socket client = null;
//                serverSocket阻塞等待客户端请求数据
                    client = serverSocket.accept();
                    if (client != null) {
                        try {
                            log.info("收到一个客户端的请求");
//                    根据客户端的Socket对象获取输入流对象
//                    封装字节流到字符流
                            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));


//                    HTTP请求由三部分组成，分别是请求行、消息报头（请求头）、请求正文
//                    1.第一行就是请求行。
                            String line = reader.readLine();
                            //line的格式如下：         GET /test.jpg /HTTP1.1
                            log.info("request line :{}", line);
//                    1.1拆分http请求路径，获取http需求请求的完成路径
                            String resource = line.substring(line.indexOf('/'), line.lastIndexOf('/') - 5);
                            log.info("the resource you request is :{}", resource);
                            resource = URLDecoder.decode(resource, "UTF-8");

//                    1.2获取请求的方法类型，比如get、post类型
                            final StringTokenizer stringTokenizer = new StringTokenizer(line);
                            String method = stringTokenizer.nextElement().toString();
                            log.info("the request method you send is :{}", method);

//                     2. 继续循环读取浏览器客户端发出的一行一行的数据
                            while ((line = reader.readLine()) != null) {
                                if (line.isEmpty()) {
//                          当line为空行的时候标志header消息结束
                                    break;
                                }
                                log.info("the http header is {}", line);
                            }

//                         如果是POST请求，则打印post请求提交的数据
                            if (Objects.equals("post", method.toLowerCase())) {
                                log.info("the post request body is: {}", reader.readLine());
                                log.info("finish read post request body.");
                            } else if (Objects.equals("get", method.toLowerCase())) {
//                            如果是get请求，则根据http请求的资源后缀名来确定返回的数据

//                            比如是下载一个图片文件，我这里直接戈丁一个图片路径来模拟下载的情况
                                if (resource.endsWith("jpg")) {
                                    transferFileHandler("/pic/123.jpg", client);
                                    closeSocket(client);
                                    continue;
                                } else {
//                            直接返回一个网页数据，其实就是讲html的代码以字节流的形式写到IO中反馈给客户端浏览器
//                            浏览器会根据Content-Type来知道反馈给浏览器的数据的格式是什么，并进行相应的处理
                                    PrintStream writer = new PrintStream(client.getOutputStream(), true);
                                    writer.println("HTTP/1.0 200 OK");
                                    writer.println("Content-Type:text/html;charset=utf-8");
                                    writer.println();
                                    writer.println("<html><body>");
                                    writer.println("<a href='www.baidu.com'>百度</a>");
                                    writer.println("<img src='https://ss0.bdstatic.com/5aV1bjqh_Q23odCf/static/superman/img/logo/bd_logo1_31bdc765.png'></img>");
                                    writer.println("/<html><body>");
//                                根据http协议，空行将结束头信息
                                    writer.println();
                                    writer.close();
                                    closeSocket(client);
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            log.info("HTTP服务器错误:{}", e.getLocalizedMessage());
                        }
                    }
                } catch (IOException e) {
                    log.info("IO exception.");
                }
            }
        }

        private void transferFileHandler(String path, Socket client){
//            对于resource路径下的，可以通过Class的getResource方法来获取
            final String resourcePath = this.getClass().getResource(path).getPath();
            File fileToSend = new File(resourcePath);
            if (fileToSend.exists() && !fileToSend.isDirectory()){
                try {
                    final PrintStream writer = new PrintStream(client.getOutputStream());
//                    返回应答消息，并结束应答
                    writer.println("HTTP/1.0 200 OK");
                    writer.println("Content-Type:application/binary");
                    writer.println("Content-Length:"+fileToSend.length());
//                    根据HTTP协议，空行将结束头信息
                    writer.println();

//                    输出内容（消息体）
                    FileInputStream fileInputStream = new FileInputStream(fileToSend);
                    byte[] buf = new byte[fileInputStream.available()];
                    fileInputStream.read(buf);
                    writer.write(buf);
                    writer.close();
                    fileInputStream.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                log.info("file is not exist: {}", path);
            }
        }

        private void closeSocket(Socket socket){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.info("{} 离开了http服务器", socket);
        }
    }

}
