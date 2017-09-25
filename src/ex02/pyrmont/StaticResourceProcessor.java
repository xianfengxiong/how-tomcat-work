package ex02.pyrmont;

import java.io.IOException;

/**
 * @since 2016-08-23
 */
public class StaticResourceProcessor {

  public void process(Request request,Response response) {
    try {
      response.sendStaticResource();
    }catch (IOException e){
      e.printStackTrace();
    }
  }
}
