import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @since 2016-08-23
 */
public class PrimitiveServlet implements Servlet {

  public void init (ServletConfig config) throws ServletException {
    System.out.println("init");
  }

  public void service (ServletRequest req, ServletResponse res)
          throws ServletException, IOException {
    System.out.println("from servlet");

    PrintWriter out = res.getWriter();
    /*
     * 原文中没有相应行,chrome不能正确处理
     */
    out.println("HTTP/1.1 200 OK");
    out.println();
    out.println("Hello.Rose are red.");
    out.print("Violets are blue.");
  }

  public void destroy () {
    System.out.println("destroy");
  }

  public String getServletInfo () {
    return null;
  }

  public ServletConfig getServletConfig () {
    return null;
  }
}
