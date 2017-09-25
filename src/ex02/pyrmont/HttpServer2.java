package ex02.pyrmont;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author xxf
 * @since 17-9-25
 */
public class HttpServer2 {

  public static final String WEB_ROOT = System.getProperty("user.dir") + File.separator + "webroot";

  private static final String SHUTDOWN_COMMAND = "/SHUTDOWN";

  private boolean shutdown;

  public static void main(String[] args) {
    HttpServer2 server = new HttpServer2();
    server.await();
  }

  public void await() {
    int port = 8080;
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }

    while (!shutdown) {
      Socket socket = null;
      InputStream is = null;
      OutputStream os = null;

      try {
        socket = serverSocket.accept();
        is = socket.getInputStream();
        os = socket.getOutputStream();

        Request request = new Request(is);
        request.parse();

        Response response = new Response(os);
        response.setRequest(request);

        if (request.getUri().startsWith("/servlet/")) {
          ServletProcessor2 processor = new ServletProcessor2();
          processor.process(request, response);
        }
        else {
          StaticResourceProcessor processor = new StaticResourceProcessor();
          processor.process(request, response);
        }

        socket.close();

        shutdown = request.getUri().equals(SHUTDOWN_COMMAND);
      }
      catch (IOException e) {
        e.printStackTrace();
        continue;
      }
    }
  }

}
