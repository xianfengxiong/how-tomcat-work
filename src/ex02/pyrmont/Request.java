package ex02.pyrmont;

import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * @since 2016-08-23
 */
public class Request implements ServletRequest {

  private InputStream input;
  private String uri;

  public Request (InputStream input) {
    this.input = input;
  }

  public String getUri() {
    return uri;
  }

  public void parse() {
    StringBuffer sb = new StringBuffer(2048);
    int i;
    byte[] buffer = new byte[2048];

    try {
      i = input.read(buffer);
    } catch (IOException e){
      e.printStackTrace();
      i = -1;
    }

    // 如果使用这种方式，需要判断i是否为-1
    sb.append(new String(buffer,0,i));

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

  public Object getAttribute (String name) {
    return null;
  }

  public Enumeration<String> getAttributeNames () {
    return null;
  }

  public String getCharacterEncoding () {
    return null;
  }

  public void setCharacterEncoding (String env) throws UnsupportedEncodingException {

  }

  public int getContentLength () {
    return 0;
  }

  public long getContentLengthLong () {
    return 0;
  }

  public String getContentType () {
    return null;
  }

  public ServletInputStream getInputStream () throws IOException {
    return null;
  }

  public String getParameter (String name) {
    return null;
  }

  public Enumeration<String> getParameterNames () {
    return null;
  }

  public String[] getParameterValues (String name) {
    return new String[0];
  }

  public Map<String,String[]> getParameterMap () {
    return null;
  }

  public String getProtocol () {
    return null;
  }

  public String getScheme () {
    return null;
  }

  public String getServerName () {
    return null;
  }

  public int getServerPort () {
    return 0;
  }

  public BufferedReader getReader () throws IOException {
    return null;
  }

  public String getRemoteAddr () {
    return null;
  }

  public String getRemoteHost () {
    return null;
  }

  public void setAttribute (String name, Object o) {

  }

  public void removeAttribute (String name) {

  }

  public Locale getLocale () {
    return null;
  }

  public Enumeration<Locale> getLocales () {
    return null;
  }

  public boolean isSecure () {
    return false;
  }

  public RequestDispatcher getRequestDispatcher (String path) {
    return null;
  }

  public String getRealPath (String path) {
    return null;
  }

  public int getRemotePort () {
    return 0;
  }

  public String getLocalName () {
    return null;
  }

  public String getLocalAddr () {
    return null;
  }

  public int getLocalPort () {
    return 0;
  }

  public ServletContext getServletContext () {
    return null;
  }

}
