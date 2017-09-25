package ex01.pyrmont;

import java.io.IOException;
import java.io.InputStream;

/**
 * @since 2016-08-23
 */
public class Request {

  private InputStream input;
  private String uri;

  public Request (InputStream input) {
    this.input = input;
  }

  public void parse() {
    StringBuffer sb = new StringBuffer(2048);
    int i;
    byte[] buffer = new byte[2048];

    /**
     * 注意:这里没有使用while循环取数据,否则会导致IO阻塞
     */
    try {
      i = input.read(buffer);
    } catch (IOException e){
      e.printStackTrace();
      i = -1;
    }

    for (int j = 0 ; j < i ; j++) {
      sb.append((char) buffer[j]);
    }

    System.out.println(sb.toString());
    uri = parseUri(sb.toString());
  }

  private String parseUri(String requestString) {
    int index1,index2;
    index1 = requestString.indexOf(' ');

    if (index1 != -1){
      index2 = requestString.indexOf(' ',index1+1);
      if (index2 > index1) {
        return requestString.substring(index1+1,index2);
      }
    }

    return null;
  }

  public String getUri() {
    return uri;
  }

}
