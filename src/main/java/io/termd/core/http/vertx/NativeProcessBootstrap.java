package io.termd.core.http.vertx;

import io.termd.core.http.ProcessBootstrap;
import io.termd.core.util.Handler;
import org.vertx.java.core.AsyncResult;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NativeProcessBootstrap extends ProcessBootstrap {

  public static void main(String[] args) throws Exception {
    SockJSBootstrap bootstrap = new SockJSBootstrap(
        "localhost",
        8080,
        new NativeProcessBootstrap());
    final CountDownLatch latch = new CountDownLatch(1);
    bootstrap.bootstrap(new Handler<AsyncResult<Void>>() {
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
