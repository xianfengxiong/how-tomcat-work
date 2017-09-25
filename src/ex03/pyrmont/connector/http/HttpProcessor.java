package ex03.pyrmont.connector.http;//package ex03.pyrmont.connector.http;
//
//import ex03.pyrmont.StaticResourceProcessor;
//import org.apache.catalina.util.StringManager;
//
//import javax.servlet.ServletException;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.net.Socket;
//
///**
// * Created by xxf on 16-8-23.
// */
//public class HttpProcessor {
//
//  public HttpProcessor(HttpConnector connector){
//    this.connector = connector;
//  }
//
//  private HttpConnector connector = null;
//  private HttpRequest request;
//  private HttpResponse response;
//  private HttpRequestLine requestLine = new HttpRequestLine();
//
//  protected StringManager sm = StringManager.getManager("ex03.pyrmont.connector.http");
//
//  public void process(Socket socket) {
//    SocketInputStream input = null;
//    OutputStream output = null;
//
//    try{
//      input = new SocketInputStream(socket.getInputStream(),2048);
//      output = socket.getOutputStream();
//
//      request = new HttpRequest(input);
//      response = new HttpResponse(output);
//      response.setRequest(request);
//
//      response.setHeader("Server","Pyrmont Servlet Container");
//
//      parseRequest(input,output);
//      parseHeader(input);
//
//      if (request.getRequestUri().startsWith("/servlet/")){
//        ServletProcessor processor = new ServletProcessor();
//        processor.process(request,response);
//      }else{
//        StaticResourceProcessor processor = new StaticResourceProcessor();
//        processor.process(request,response);
//      }
//
//      socket.close();
//    } catch (IOException e){
//      e.printStackTrace();
//    }
//  }
//
//  private void parseRequest(SocketInputStream input,OutputStream output) throws IOException, ServletException {
//    input.readRequestLine(requestLine);
//    String method = new String(requestLine.method,0,requestLine.methodEnd);
//    String uri = null;
//    String protocal = new String(requestLine.protocal,0,requestLine.protocalEnd);
//
//    if (method.length() < 1) {
//      throw new ServletException("Miss HTTP request method");
//    }else if(requestLine.uriEnd < 1) {
//      throw new ServletException("Miss HTTP request URI");
//    }
//
//    int question = requestLine.indexOf("?");
//    if (question >= 0) {
//      request.setQueryString(requestLine.uri,question + 1,
//          requestLine.uriEnd -question -1);
//      uri = new String(requestLine.uri,0,question);
//    }else{
//      request.setQueryString(null);
//      uri = new String(requestLine.uri,0,requestLine.uriEnd);
//    }
//
//    if (!uri.startsWith("/")){
//      int pos = uri.indexOf("://");
//      if (pos != -1){
//        pos = uri.indexOf('/',pos+3);
//        if (pos==-1) {
//          uri = "";
//        }else{
//          uri = uri.substring(pos);
//        }
//      }
//    }
//
//    String match = ";jsessionid=";
//    int semicolon = uri.indexOf(match);
//    if (semicolon >= 0){
//      String rest = uri.substring(semicolon+match.length());
//      int semicolon2 = rest.indexOf(';');
//      if (semicolon2 >= 0){
//        request.setRequestedSessionId(rest.substring(0, semicolon2));
//        rest = rest.substring(semicolon2);
//      }else{
//        request.setRequestedSessionId(rest);
//        rest = "";
//      }
//      request.setRequestedSessionURL(true);
//      uri = uri.substring(0,semicolon) + rest;
//    }else{
//      request.setRequestedSessionId(null);
//      request.setRequestedSessionURL(false);
//    }
//
//    String normalizedUri = normalize(uri);
//    request.setMethod(method);
//    request.setProtocal(protocal);
//    if (normalizedUri != null){
//      request.setRequestURI(normalizedUri);
//    }else{
//      request.setRequestURI(uri);
//    }
//    if (normalizedUri == null){
//      throw new ServletException("Invalid URI:"+uri);
//    }
//  }
//
//}
