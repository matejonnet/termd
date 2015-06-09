package io.termd.core.http.undertow;

import io.termd.core.http.AbstractTtyConnection;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketTtyConnection extends AbstractTtyConnection implements TtyConnection {

  private static Logger log = LoggerFactory.getLogger(WebSocketTtyConnection.class);

  private WebSocketChannel webSocketChannel;
  private final Executor executor;

  public WebSocketTtyConnection(final WebSocketChannel webSocketChannel, Executor executor) {
    this.webSocketChannel = webSocketChannel;
    this.executor = executor;

    ChannelListener<WebSocketChannel> listener = new AbstractReceiveListener() {

      @Override
      protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        log.trace("Server received full binary message");
        Pooled<ByteBuffer[]> pulledData = message.getData();
        try {
          ByteBuffer[] resource = pulledData.getResource();
          ByteBuffer byteBuffer = WebSockets.mergeBuffers(resource);
          String msg = new String(byteBuffer.array());
          log.trace("Sending message to decoder: {}", msg);
          writeToDecoder(msg);
        } finally {
          pulledData.discard();
        }
      }
    };
    webSocketChannel.getReceiveSetter().set(listener);
    webSocketChannel.resumeReceives();
  }

  protected Handler<byte[]> onByteHandler() {
    return (bytes) -> WebSockets.sendBinary(ByteBuffer.wrap(bytes), webSocketChannel, null);
  }

  public void schedule(final Runnable task) {
    executor.execute(task);
  }

}
