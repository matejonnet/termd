package io.termd.core.http.vertx;

import io.termd.core.http.AbstractTtyConnection;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import org.vertx.java.core.Context;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection extends AbstractTtyConnection implements TtyConnection {

  private final SockJSSocket socket;
  private Dimension size = null;
  private Handler<Dimension> resizeHandler;
  private final Context context;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(final Runnable command) {
      context.runOnContext(new org.vertx.java.core.Handler<Void>() {
        @Override
        public void handle(Void event) {
          command.run();
        }
      });
    }
  });

  public SockJSTtyConnection(Vertx vertx, SockJSSocket socket) {
    this.socket = socket;
    this.context = vertx.currentContext();

    socket.dataHandler(new org.vertx.java.core.Handler<Buffer>() {
      @Override
      public void handle(Buffer msg) {
        writeToDecoder(msg.toString());
      }
    });
  }

  @Override
  public void schedule(final Runnable task) {
    context.runOnContext(new org.vertx.java.core.Handler<Void>() {
      @Override
      public void handle(Void v) {
        task.run();
      }
    });
  }

  @Override
  protected Handler<byte[]> onByteHandler() {
    return new Handler<byte[]>() {
      @Override
      public void handle(byte[] event) {
        socket.write(new Buffer(event));
      }
    };
  }
}
