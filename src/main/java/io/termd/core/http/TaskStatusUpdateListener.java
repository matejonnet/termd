package io.termd.core.http;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@FunctionalInterface
public interface TaskStatusUpdateListener {
  public void statusUpdated(TaskStatusUpdateEvent statusUpdateEvent);
}
