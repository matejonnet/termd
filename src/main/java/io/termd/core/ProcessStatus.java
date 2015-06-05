package io.termd.core;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ProcessStatus {

    private Status status;

    public ProcessStatus(Status status) {
        this.status = status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public String toJson() {
        return "{\"status\" : \"" + status + "\"}";
    }
}
