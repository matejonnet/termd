package io.termd.core.http;

import io.termd.core.Status;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TaskStatusUpdateEvent {
  private Task task;
  private Status oldStatus;
  private Status newStatus;

  public TaskStatusUpdateEvent(Task task, Status oldStatus, Status newStatus) {
    this.task = task;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
  }

  public String toJson() {
    return "{\"task\" : \"" + task.getId() + "\", \"old-status\" : \"" + oldStatus.toString() + "\", \"new-status\" : \"" + newStatus.toString() + "\"}";
  }

}
