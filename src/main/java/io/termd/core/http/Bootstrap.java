package io.termd.core.http;

import io.termd.core.readline.KeyDecoder;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import io.termd.core.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Bootstrap implements Handler<TtyConnection> {

  Logger log = LoggerFactory.getLogger(Bootstrap.class);

  private TaskStatusUpdateListener taskStatusUpdateListener;

  public Bootstrap() {
    this((taskStatusUpdateEvent) -> {});
  }

  public Bootstrap(TaskStatusUpdateListener taskStatusUpdateListener) {
    this.taskStatusUpdateListener = taskStatusUpdateListener;
  }

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
        Task task = new Task(Bootstrap.this, conn, readline, line, taskStatusUpdateListener);
        task.start();
      }
    };
    readline.readline(conn, "% ", requestHandler);
  }
}
