package ex01.pyrmont;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * * HTTP Response = Status-Line
 * *((general-header | response-header | entity-header)CRLF)
 * CRLF
 * [message-body]
 * Status-Line = HTTP-Version SP Status-Code SP Reson-Phrase CRLF
 *
 * @since 2016-08-23
 */
public class Response {

  private static final int BUFFER_SIZE = 1024;
  private OutputStream output;
  private Request request;

  public Response(OutputStream output) {
    this.output = output;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public void sendStaticResource() {
    FileInputStream fis = null;
    /*
     * 注意：原文的发送文件并不是一个完整的响应,缺少响应行和响应头
     * chrome,safari都不能处理响应,作如下修改
     */
    try{
      File file = new File(HttpServer.WEB_ROOT,request.getUri());
      if (file.exists()) {
        byte[] buffer = new byte[(int)file.length()];
        fis = new FileInputStream(file);
        int len = fis.read(buffer, 0, buffer.length);
        /*
         * chrome不需要Content-Type和Content-Length也可以正确显示
         */
        output.write(("HTTP/1.1 200 OK\r\n" +
//            "Content-Type:text/html\r\n" +
//            "Content-Length:" + len + "\r\n" +
            "\r\n").getBytes());
        output.write(buffer,0,len);
      } else {
        String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
            "Content-Type:text/html\r\n" +
            "Content-Length:23\r\n" +
            "\r\n" +
            "<h1>File Not Found</h1>";
        output.write(errorMessage.getBytes());
      }
    }catch (IOException e) {
      System.out.println(e.toString());
    }finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }

    // region 书中的原代码
    /*
    try {
      File file = new File(HttpServer.WEB_ROOT, request.getUri());
      byte[] buffer = new byte[BUFFER_SIZE];
      if (file.exists()) {
        fis = new FileInputStream(file);
        int len = fis.read(buffer, 0, BUFFER_SIZE);
        while (len != -1) {
          output.write(buffer, 0, len);
          len = fis.read(buffer, 0, BUFFER_SIZE);
        }
      } else {
        String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
            "Content-Type:text/html\r\n" +
            "Content-Length:23\r\n" +
            "\r\n" +
            "<h1>File Not Found</h1>";
        output.write(errorMessage.getBytes());
      }
    } catch (IOException e) {
      System.out.println(e.toString());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
    */
    // endregion
  }
}
