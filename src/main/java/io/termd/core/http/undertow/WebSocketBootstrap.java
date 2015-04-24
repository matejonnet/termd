package io.termd.core.http.undertow;

import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import io.undertow.Undertow;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import org.vertx.java.core.json.JsonObject;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketBootstrap {

  final String host;
  final int port;
  final Handler<TtyConnection> handler;

  public WebSocketBootstrap(String host, int port, Handler<TtyConnection> handler) {
    this.host = host;
    this.port = port;
    this.handler = handler;
  }

  public void bootstrap(final Consumer<Boolean> completionHandler) {

    HttpHandler httpHandler = new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception {
        WebSocketBootstrap.this.handleRequest(exchange);

        JsonObject config = new JsonObject().putString("prefix", "/term"); //TODO
        WebSocketChannel webSocketChannel;

        WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, exchange);
        handler.handle(conn);
      }
    };

    Undertow undertow = Undertow.builder()
      .addHttpListener(port, host)
      .setHandler(httpHandler)
      .build();

    undertow.start();



    completionHandler.accept(true);
  }

  private void handleRequest(HttpServerExchange exchange) throws URISyntaxException {
    String requestPath = exchange.getRequestPath();
    Sender responseSender = exchange.getResponseSender();

    if ("/".equals(requestPath)) {
      requestPath = "/index.html";
    }
    URL res = WebSocketBootstrap.class.getResource("/io/termd/core/http" + requestPath);

    try {
      if (res != null) {
        Path resource = Paths.get(res.toURI());
        IoCallback onComplete = null; //TODO
        responseSender.transferFrom(FileChannel.open(resource), onComplete);
      } else {
        exchange.setResponseCode(404);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      responseSender.close();
    }
  }

}
