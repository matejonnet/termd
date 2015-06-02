package io.termd.core.http.undertow;

import io.termd.core.http.IoUtils;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketBootstrap {

  final String host;
  final int port;
  final Handler<TtyConnection> termdHandler;
  private final Executor executor = Executors.newFixedThreadPool(1);

  public WebSocketBootstrap(String host, int port, Handler<TtyConnection> termdHandler) {
    this.host = host;
    this.port = port;
    this.termdHandler = termdHandler;
  }

  public void bootstrap(final Handler<Boolean> completionHandler) {

    HttpHandler httpHandler = new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception {
        WebSocketBootstrap.this.handleRequest(exchange);
//        JsonObject config = new JsonObject().putString("prefix", "/term"); //TODO
      }
    };

    Undertow undertow = Undertow.builder()
//      .setIoThreads(2)
//      .setWorkerThreads(1)
      .addHttpListener(port, host)
      .setHandler(httpHandler)
      .build();

    undertow.start();

    completionHandler.handle(true);
  }

  private void handleRequest(HttpServerExchange exchange) throws Exception {
    String requestPath = exchange.getRequestPath();
    Sender responseSender = exchange.getResponseSender();

    if ("/".equals(requestPath)) {
      requestPath = "/index.html";
    }
    if (requestPath.equals("/term")) {
      getWebSocketHandler().handleRequest(exchange);
      return;
    }

    try {
      String resourcePath = "io/termd/core/http" + requestPath;
      String content = IoUtils.readResource(resourcePath, this.getClass().getClassLoader());
      responseSender.send(content);
    } catch (Exception e) {
      e.printStackTrace();
      exchange.setResponseCode(404);
    } finally {
      responseSender.close();
    }
  }

  private HttpHandler getWebSocketHandler() {
    WebSocketConnectionCallback webSocketConnectionCallback = new WebSocketConnectionCallback() {
      @Override
      public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel webSocketChannel) {
        WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, executor);
        termdHandler.handle(conn);
      }
    };

    HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
    return webSocketHandshakeHandler;

  }

  private void registerListener(WebSocketChannel webSocketChannel) {
    ChannelListener<WebSocketChannel> listener = new AbstractReceiveListener() {

      @Override
      public void handleEvent(WebSocketChannel channel) {
        System.out.println("## Handling server receive event ... ");
        super.handleEvent(channel);
      }

      @Override
      protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        System.out.println("# Server received full binary message"); //TODO log
        Pooled<ByteBuffer[]> pulledData = message.getData();
        try {
          ByteBuffer[] resource = pulledData.getResource();
          ByteBuffer byteBuffer = WebSockets.mergeBuffers(resource);
          String msg = new String(byteBuffer.array());
        } finally {
          pulledData.discard();
        }
      }

      @Override
      protected void onFullTextMessage (WebSocketChannel channel, BufferedTextMessage message) throws IOException {
        System.out.println("# Server received full binary message"); //TODO log
      }

      @Override
      protected void onText(WebSocketChannel webSocketChannel, StreamSourceFrameChannel messageChannel) throws IOException {
        System.out.println("# Server received text message ... ");
        bufferFullMessage(messageChannel);
      }

      @Override
      protected void onBinary(WebSocketChannel webSocketChannel, StreamSourceFrameChannel messageChannel) throws IOException {
        System.out.println("# Server received binary message ... ");
        bufferFullMessage(messageChannel);
      }

    };
    System.out.println("Server is setting receive listener ...");
    webSocketChannel.getReceiveSetter().set(listener);
    webSocketChannel.resumeReceives();
  }

}
