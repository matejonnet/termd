package io.termd.core.http.undertow;

import io.termd.core.http.IoUtils;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketTtyConnection implements TtyConnection {

  private static Logger log = LoggerFactory.getLogger(WebSocketTtyConnection.class);

  private final WebSocketChannel socket;
  private Dimension size = null;
  private Handler<Dimension> resizeHandler;

  private final Executor executor;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(final Runnable command) {
      System.out.println("Server read buffer executing command:" + command); //TODO log
      executor.execute(command);
    }
  });
  private final SignalDecoder signalDecoder = new SignalDecoder(3).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, signalDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Handler<byte[]>() {
    @Override
    public void handle(byte[] bytes) {
        WebSocketCallback<Void> onComplete = null; //TODO on complete
        WebSockets.sendBinary(ByteBuffer.wrap(bytes), socket, onComplete);
    }
  });

  public WebSocketTtyConnection(final WebSocketChannel webSocketChannel, Executor executor) {
    this.socket = webSocketChannel;
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
          IoUtils.writeToDecoder(decoder, msg);
        } finally {
          pulledData.discard();
        }
      }

      @Override
      protected void onFullTextMessage (WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                log.trace("Server received full binary message: {}", message.getData());
        IoUtils.writeToDecoder(decoder, message.getData());
      }
    };
    webSocketChannel.getReceiveSetter().set(listener);
    webSocketChannel.resumeReceives();
  }

  @Override
  public Handler<String> getTermHandler() {
    return null; //TODO
  }

  @Override
  public void setTermHandler(Handler<String> handler) {
    //TODO
  }

  @Override
  public Handler<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  @Override
  public void setResizeHandler(Handler<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.handle(size);
    }
  }

  @Override
  public void schedule(final Runnable task) {
    executor.execute(task);
  }

  @Override
  public Handler<Signal> getSignalHandler() {
    return signalDecoder.getSignalHandler();
  }

  @Override
  public void setSignalHandler(Handler<Signal> handler) {
    signalDecoder.setSignalHandler(handler);
  }

  @Override
  public Handler<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setReadHandler(Handler<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Handler<int[]> writeHandler() {
    return encoder;
  }
}
