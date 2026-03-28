package org.chekhov.http_poller_ui.service;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;
import org.chekhov.http_poller_ui.model.PollResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PollerService {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private String url;
    private int timeoutMs;
    private int delayMs;
    private Map<String, String> headers;
    private Duration pollingMinutes;

    private Thread pollerThread;
    @Getter
    private final BooleanProperty isConfigured = new SimpleBooleanProperty(false);

    public void configure(String url, int timeoutMs, int delayMs, Duration pollingMinutes, Map<String, String> headers) {
        this.url = url;
        this.timeoutMs = timeoutMs;
        this.delayMs = delayMs;
        this.pollingMinutes = pollingMinutes;
        this.headers = headers;

        this.isConfigured.set(true);
    }

    public void start(
            Consumer<PollResult> onResult,
            Runnable onFinished
    ) {
        running.set(true);

        pollerThread = Thread.ofVirtual().start(() -> {
            long sessionEndMs = System.currentTimeMillis() + pollingMinutes.toMillis();

            while (running.get() && System.currentTimeMillis() < sessionEndMs) {
                PollResult result = doRequest();

                Platform.runLater(() -> onResult.accept(result));

                if (running.get() && System.currentTimeMillis() < sessionEndMs) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException _) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            Platform.runLater(onFinished);
        });
    }

    public void stop() {
        running.set(false);
        this.isConfigured.set(false);
        if (pollerThread != null) {
            pollerThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private PollResult doRequest() {
        try {
            HttpRequest.Builder builder = buildRequest();

            long startTime = System.currentTimeMillis();

            HttpResponse<Void> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.discarding()
            );

            long elapsed = System.currentTimeMillis() - startTime;

            return new PollResult(response.statusCode(), elapsed);
        } catch (HttpTimeoutException e) {
            return new PollResult(0, timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new PollResult(0, 0);
        }
        catch (Exception e) {
            return new PollResult(0, 0);
        }
    }

    private HttpRequest.Builder buildRequest() {
        HttpRequest.Builder httpBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofMillis(timeoutMs));

        buildHeaders(httpBuilder);

        return httpBuilder;
    }

    private void buildHeaders(HttpRequest.Builder httpBuilder) {
        Map<String, String> headersMap = Map.copyOf(this.headers);

        if (headersMap.isEmpty()) return;

        for (Map.Entry<String, String> entry : headersMap.entrySet()) {
            httpBuilder.setHeader(entry.getKey(), entry.getValue());
        }
    }
}
