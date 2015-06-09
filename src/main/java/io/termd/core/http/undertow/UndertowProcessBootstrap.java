package io.termd.core.http.undertow;

import io.termd.core.http.ProcessBootstrap;
import io.termd.core.util.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowProcessBootstrap extends ProcessBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowProcessBootstrap.class);

  public static void main(String[] args) throws Exception {
    start("localhost", 8080, null);
  }

  public static void start(String host, int port, final Runnable onStart) throws InterruptedException {
    WebSocketBootstrap bootstrap = new WebSocketBootstrap(
        host,
        port,
        new UndertowProcessBootstrap());
    bootstrap.bootstrap(new Handler<Boolean>() {
      @Override
      public void handle(Boolean event) {
        if (event) {
          System.out.println("Server started on " + 8080);
          if (onStart != null) onStart.run();
        } else {
          System.out.println("Could not start");
        }
      }
    });
  }
}
