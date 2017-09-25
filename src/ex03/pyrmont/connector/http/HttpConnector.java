package ex03.pyrmont.connector.http;//package ex03.pyrmont.connector.http;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//
///**
// * Created by xxf on 16-8-23.
// */
//public class HttpConnector implements Runnable {
//
//  private boolean stopped;
//  private String scheme = "http";
//
//  public String getScheme(){
//    return scheme;
//  }
//
//  public void run() {
//    ServerSocket serverSocket = null;
//    int port  = 8080;
//    try {
//      serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
//    }catch (IOException e){
//      e.printStackTrace();
//      System.exit(1);
//    }
//
//    while(!stopped){
//      Socket socket = null;
//      try {
//        socket = serverSocket.accept();
//      } catch (IOException e) {
//        continue;
//      }
//
//      HttpProcessor processor = new HttpProcessor(this);
//      processor.process(socket);
//    }
//  }
//
//  public void start(){
//    Thread thread = new Thread(this);
//    thread.start();
//  }
//
//}
