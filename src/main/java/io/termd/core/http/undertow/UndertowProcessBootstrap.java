package io.termd.core.http.undertow;

import io.termd.core.http.Bootstrap;
import io.termd.core.http.Task;
import io.termd.core.http.TaskStatusUpdateListener;
import io.termd.core.util.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowProcessBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowProcessBootstrap.class);

  private final List<Task> runningTasks = new ArrayList<>();

  public static void main(String[] args) throws Exception {
    new UndertowProcessBootstrap().start("localhost", 8080, null);
  }

  public void start(String host, int port, final Runnable onStart) throws InterruptedException {
    Bootstrap bootstrap = new Bootstrap();
    bootstrap.addStatusUpdateListener(getStatusUpdateListener());

    WebSocketBootstrap webSocketBootstrap = new WebSocketBootstrap(host, port, bootstrap, runningTasks);

    webSocketBootstrap.bootstrap(new Handler<Boolean>() {
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

  private TaskStatusUpdateListener getStatusUpdateListener() {
    return (statusUpdateEvent) -> {
      switch (statusUpdateEvent.getNewStatus()) {
        case RUNNING:
          runningTasks.add(statusUpdateEvent.getTask());
          break;

        case SUCCESSFULLY_COMPLETED:
        case FAILED:
        case INTERRUPTED:
          runningTasks.remove(statusUpdateEvent.getTask());
      }

    };
  }
}
