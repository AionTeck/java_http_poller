package org.chekhov.http_poller_ui.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PollResult {
    public enum Status {
        OK,
        ERROR,
        TIMEOUT
    }

    private final LocalTime time;

    @Getter
    private final int statusCode;

    @Getter
    private final long responseMs;

    @Getter
    private final Status status;

    @Getter
    @Setter
    private long avgResponseMs;

    public PollResult(int statusCode, long responseMs) {
        this.time = LocalTime.now();
        this.statusCode = statusCode;
        this.responseMs = responseMs;

        if (statusCode >= 200 && statusCode < 300) {
            this.status = Status.OK;
        } else if (statusCode >= 400) {
            this.status = Status.ERROR;
        } else {
            this.status = Status.TIMEOUT;
        }
    }

    public String getFormattedTime() {
        return time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public String getStatusLabel() {
        return statusCode == 0 ? "TIMEOUT" : String.valueOf(statusCode);
    }
}
