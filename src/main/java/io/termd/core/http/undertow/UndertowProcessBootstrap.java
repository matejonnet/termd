package io.termd.core.http.undertow;

import io.termd.core.readline.KeyDecoder;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import io.termd.core.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowProcessBootstrap implements Handler<TtyConnection> {

  Logger log = LoggerFactory.getLogger(UndertowProcessBootstrap.class);
  private static List<Task> runningTasks = new ArrayList<>(); //TODO keep "short" history but remove "old" completed tasks. Use evicting queue instead of list

    @Override
  public void handle(final TtyConnection conn) {
    InputStream inputrc = KeyDecoder.class.getResourceAsStream("inputrc");
    Keymap keymap = new Keymap(inputrc);
    Readline readline = new Readline(keymap);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      log.trace("Server is adding function to readline: {}", function);

      readline.addFunction(function);
    }
    conn.setTermHandler(new Handler<String>() {
      @Override
      public void handle(String term) {
        // Not used yet but we should propagage this to the process builder
        System.out.println("CLIENT $TERM=" + term);
      }
    });
    conn.writeHandler().handle(Helper.toCodePoints("Welcome sir\r\n"));
    read(conn, readline);
  }

  public void read(final TtyConnection conn, final Readline readline) {
    Handler<String> requestHandler = new Handler<String>() {
      @Override
      public void handle(String line) {
        Task task = new Task(UndertowProcessBootstrap.this, conn, readline, line);
        runningTasks.add(task);
        task.start();
      }
    };
    readline.readline(conn, "% ", requestHandler);
  }

    public static void main(String[] args) throws Exception {
    start("localhost", 8080, null);
  }

  public static void start(String host, int port, final Runnable onStart) throws InterruptedException {
    WebSocketBootstrap bootstrap = new WebSocketBootstrap(
        host,
        port,
        new UndertowProcessBootstrap(), runningTasks);
    bootstrap.bootstrap(new Handler<Boolean>() {
      @Override
      public void handle(Boolean event) {
        if (event) {
          System.out.println("Server started on " + 8080);
          if (onStart != null) onStart.run();
        } else {
          System.out.println("Could not start");
        }
      }
    });
  }
}
