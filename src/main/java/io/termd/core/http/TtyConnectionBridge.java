package io.termd.core.http;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TtyConnectionBridge implements TtyConnection {

  private static Logger log = LoggerFactory.getLogger(TtyConnectionBridge.class);
  private final Executor executor;

  private Dimension size = null;

  private Handler<Dimension> resizeHandler;

  private final ReadBuffer readBuffer;

  private final SignalDecoder onCharSignalDecoder;
  private final BinaryDecoder decoder;
  private final BinaryEncoder encoder;

  public TtyConnectionBridge(Handler<byte[]> onByteHandler, Executor executor) {
    this.executor = executor;
    readBuffer = new ReadBuffer(new Executor() {
      @Override
      public void execute(final Runnable command) {
        log.debug("Server read buffer executing command: {}" + command);
        schedule(command);
      }
    });

    onCharSignalDecoder = new SignalDecoder(3).setReadHandler(readBuffer);
    decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, onCharSignalDecoder);
    encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, onByteHandler);
  }

  public void writeToDecoder(String msg) throws DecodeException {
    JsonObject obj = new JsonObject(msg.toString());
    switch (obj.getString("action")) {
      case "read":
        String data = obj.getString("data");
        decoder.write(data.getBytes());
        break;
    }
  }

  public Handler<String> getTermHandler() {
    return null; //TODO
  }

  public void setTermHandler(Handler<String> handler) {
      //TODO
  }

  public Handler<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  public void setResizeHandler(Handler<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.handle(size);
    }
  }

  public Handler<Signal> getSignalHandler() {
    return onCharSignalDecoder.getSignalHandler();
  }

  public void setSignalHandler(Handler<Signal> handler) {
    onCharSignalDecoder.setSignalHandler(handler);
  }

  public Handler<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  public void setReadHandler(Handler<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  public Handler<int[]> writeHandler() {
    return encoder;
  }

  @Override
  public void schedule(Runnable task) {
    executor.execute(task);
  }
}
