package ex02.pyrmont;

import javax.servlet.Servlet;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

/**
 * @author xxf
 * @since 17-9-25
 */
public class ServletProcessor2 {

  public void process(Request request, Response response) {
    String uri = request.getUri();
    String servletName = uri.substring(uri.lastIndexOf("/") + 1);
    URLClassLoader loader = null;

    try {
      URL[] urls = new URL[1];
      URLStreamHandler handler = null;
      File classPath = new File(HttpServer2.WEB_ROOT);
      String repository = (new URL("file", null, classPath.getCanonicalPath() + File.separator)).toString();

      urls[0] = new URL(null, repository, handler);
      loader = new URLClassLoader(urls);
    }
    catch (IOException e) {
      e.printStackTrace();
    }

    Class myClass = null;
    try {
      myClass = loader.loadClass(servletName);
    }
    catch (ClassNotFoundException e) {
      System.out.println(e.toString());
    }

    Servlet servlet = null;
    RequestFacade requestFacade = new RequestFacade(request);
    ResponseFacade responseFacade = new ResponseFacade(response);

    try {
      servlet = (Servlet) myClass.newInstance();
      servlet.service(requestFacade, responseFacade);
    }
    catch (Exception e) {
      System.out.println(e.toString());
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }

}
