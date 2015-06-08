package io.termd.core.http.undertow;

import io.termd.core.http.ProcessBootstrap;
import io.termd.core.http.Task;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketBootstrap {

  final String host;
  final int port;
  final ProcessBootstrap termdHandler;
  private final Executor executor = Executors.newFixedThreadPool(1);

  public WebSocketBootstrap(String host, int port, ProcessBootstrap termdHandler) {
    this.host = host;
    this.port = port;
    this.termdHandler = termdHandler;
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
      String content = readResource(resourcePath, this.getClass().getClassLoader());
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
          List<Task> tasks = termdHandler.getRunningTasks();
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

  private String readResource(String name, ClassLoader classLoader) throws IOException {
    String configString;
    InputStream is = classLoader.getResourceAsStream(name);
    if (is == null) {
      throw new IOException("Cannot read resource:" + name);
    }
    try {
      configString = new Scanner(is, Charset.defaultCharset().name()).useDelimiter("\\A").next();
    } finally {
      if (is != null) {
        is.close();
      }
    }
    return configString;
  }

}
