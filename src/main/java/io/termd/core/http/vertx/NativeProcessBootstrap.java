package io.termd.core.http.vertx;

import io.termd.core.http.Bootstrap;
import io.termd.core.util.Handler;
import org.vertx.java.core.AsyncResult;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NativeProcessBootstrap {

  public static void main(String[] args) throws Exception {
    Bootstrap bootstrap = new Bootstrap();
    SockJSBootstrap sockJSBootstrap = new SockJSBootstrap(
        "localhost",
        8080,
        bootstrap);
    final CountDownLatch latch = new CountDownLatch(1);
    sockJSBootstrap.bootstrap(new Handler<AsyncResult<Void>>() {
      @Override
      public void handle(AsyncResult<Void> event) {
        if (event.succeeded()) {
          System.out.println("Server started on " + 8080);
        } else {
          System.out.println("Could not start");
          event.cause().printStackTrace();
          latch.countDown();
        }
      }
    });
    latch.await();
  }
}
