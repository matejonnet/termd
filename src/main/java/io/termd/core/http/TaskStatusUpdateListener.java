package io.termd.core.http;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@FunctionalInterface
public interface TaskStatusUpdateListener extends Consumer<TaskStatusUpdateEvent> {

  @Override
  void accept(TaskStatusUpdateEvent statusUpdateEvent);
}
