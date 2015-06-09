package io.termd.core.http.vertx;

import io.termd.core.http.TtyConnectionBridge;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.util.Handler;
import org.vertx.java.core.Context;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection {

  private final SockJSSocket socket;
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
  private final TtyConnectionBridge ttyConnection;

  public SockJSTtyConnection(Vertx vertx, SockJSSocket socket) {
    ttyConnection = new TtyConnectionBridge(onByteHandler(), (task) -> schedule(task));


    this.socket = socket;
    this.context = vertx.currentContext();

    socket.dataHandler(new org.vertx.java.core.Handler<Buffer>() {
      @Override
      public void handle(Buffer msg) {
        ttyConnection.writeToDecoder(msg.toString());
      }
    });
  }

  private void schedule(final Runnable task) {
    context.runOnContext(new org.vertx.java.core.Handler<Void>() {
      @Override
      public void handle(Void v) {
        task.run();
      }
    });
  }

  private Handler<byte[]> onByteHandler() {
    return new Handler<byte[]>() {
      @Override
      public void handle(byte[] event) {
        socket.write(new Buffer(event));
      }
    };
  }

  public TtyConnectionBridge getTtyConnection() {
    return ttyConnection;
  }
}
