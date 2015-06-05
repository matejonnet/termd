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
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.vertx.java.core.json.JsonObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketBootstrap {

  final String host;
  final int port;
  final Handler<TtyConnection> termdHandler;
  private final Executor executor = Executors.newFixedThreadPool(1);
  private final List<Task> tasks;

  public WebSocketBootstrap(String host, int port, Handler<TtyConnection> termdHandler, List<Task> tasks) {
    this.host = host;
    this.port = port;
    this.termdHandler = termdHandler;
    this.tasks = tasks;
  }

  public void bootstrap(final Handler<Boolean> completionHandler) {

    HttpHandler httpHandler = new HttpHandler() {
      @Override
      public void handleRequest(HttpServerExchange exchange) throws Exception {
        WebSocketBootstrap.this.handleRequest(exchange);
      }
    };

    Undertow undertow = Undertow.builder()
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
    if (requestPath.equals("/processes")) {
      getProcessStatusHandler().handleRequest(exchange);
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

  private HttpHandler getProcessStatusHandler() {
    return new HttpHandler() {
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            Map<String, Object> tasksMap = tasks.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), t -> t.getProcessStatus().getStatus().toString()));
            JsonObject jsonObject = new JsonObject(tasksMap);
            exchange.getResponseSender().send(jsonObject.toString());
        }
    };
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
}
