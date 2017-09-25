package ex01.pyrmont;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @since 2016-08-23
 */
public class HttpServer {

  public static final String WEB_ROOT =
      System.getProperty("user.dir") + File.separator + "webroot";

  public static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  private boolean shutdown = false;

  public static void main (String[] args) {
    HttpServer server = new HttpServer();
    server.await();
  }

  public void await(){
    ServerSocket serverSocket = null;
    int port = 8080;
    try {
      serverSocket = new ServerSocket(port,1, InetAddress.getByName("127.0.0.1"));
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }

    while(!shutdown){
      Socket socket = null;
      InputStream input = null;
      OutputStream output = null;

      try {
        socket = serverSocket.accept();
        input = socket.getInputStream();
        output = socket.getOutputStream();

        Request request = new Request(input);
        /*
         * 注意,parse方法没有使用while循环取数据,
         * 因为socket没有断开,如果使用while取数据
         * 会导致线程一直阻塞在这里,直到socket断开
         * 导致下面的response一直不能写数据
         */
        request.parse();

        Response response = new Response(output);
        response.setRequest(request);
        response.sendStaticResource();

        socket.close();

        shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
      } catch (IOException e) {
        e.printStackTrace();
        continue;
      }
    }
  }
}
