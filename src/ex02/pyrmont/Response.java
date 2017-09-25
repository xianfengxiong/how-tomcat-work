package ex02.pyrmont;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import java.io.*;
import java.util.Locale;

/**
 * @since 2016-08-23
 */
public class Response implements ServletResponse{

  private static final int BUFFER_SIZE = 1024;
  Request request;
  OutputStream output;
  PrintWriter writer;

  public Response (OutputStream output) {
    this.output = output;
  }

  public void setRequest(Request request) {
    this.request = request;
  }

  public void sendStaticResource() throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    FileInputStream fis = null;
    try {
      File file = new File(HttpServer1.WEB_ROOT, request.getUri());
      fis = new FileInputStream(file);
      output.write(("HTTP/1.1 200 OK\r\n" +
          "\r\n").getBytes());
      int len = fis.read(buffer, 0, BUFFER_SIZE);
      while (len != -1) {
        output.write(buffer, 0, len);
        len = fis.read(buffer, 0, BUFFER_SIZE);
      }
    } catch (FileNotFoundException e){
      String errorMessage = "HTTP/1.1 404 File Not Found\r\n" +
          "Content-Type:text/html\r\n" +
          "Content-Length:23\r\n" +
          "\r\n" +
          "<h1>File Not Found</h1>";
      output.write(errorMessage.getBytes());
    } finally {
      if (fis!=null){
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public String getCharacterEncoding () {
    return null;
  }

  public String getContentType () {
    return null;
  }

  public ServletOutputStream getOutputStream () throws IOException {
    return null;
  }

  public PrintWriter getWriter () throws IOException {
    // 多次调用的话每次都new一个PrintWriter会有问题么？
    writer = new PrintWriter(output,true);
    return writer;
  }

  public void setCharacterEncoding (String charset) {

  }

  public void setContentLength (int len) {

  }

  public void setContentLengthLong (long len) {

  }

  public void setContentType (String type) {

  }

  public void setBufferSize (int size) {

  }

  public int getBufferSize () {
    return 0;
  }

  public void flushBuffer () throws IOException {

  }

  public void resetBuffer () {

  }

  public boolean isCommitted () {
    return false;
  }

  public void reset () {

  }

  public void setLocale (Locale loc) {

  }

  public Locale getLocale () {
    return null;
  }
}
