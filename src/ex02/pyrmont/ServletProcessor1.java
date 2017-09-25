package ex02.pyrmont;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * @since 2016-08-23
 */
public class ServletProcessor1 {

  public void process(Request request,Response response){
    String uri = request.getUri();
    String servletName = uri.substring(uri.lastIndexOf("/")+1);
    URLClassLoader loader = null;

    try {
      URL[] urls = new URL[1];
      URLStreamHandler streamHandler = null;
      File classPath = new File(HttpServer1.WEB_ROOT);
      String repository =
          (new URL("file", null, classPath.getCanonicalPath() +
              File.separator)).toString(); // 路径最后必须加上 /

      urls[0] = new URL(null, repository, streamHandler);
      /* 参考URLClassLoader的注释
       * Any URL that ends with
       * a '/' is assumed to refer to a directory. Otherwise, the URL is
       * assumed to refer to a JAR file which will be downloaded and opened
       * as needed.
       */
      loader = new URLClassLoader(urls);
    }catch (IOException e){
      System.out.println(e.toString());
    }

    Class myClass = null;
    try{
      myClass = loader.loadClass(servletName);
    } catch (ClassNotFoundException e) {
      System.out.println(e.toString());
    }

    Servlet servlet = null;

    try{
      servlet = (Servlet)myClass.newInstance();
      servlet.service(request,response);
    }catch (Exception e){
      System.out.println(e.toString());
    }catch (Throwable e){
      System.out.println(e.toString());
    }
  }
}
