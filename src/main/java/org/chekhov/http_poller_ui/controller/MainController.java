package org.chekhov.http_poller_ui.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Duration;
import org.chekhov.http_poller_ui.model.HeaderEntry;
import org.chekhov.http_poller_ui.model.PollResult;
import org.chekhov.http_poller_ui.service.PollerService;

import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

public class MainController {
    @FXML private TextField urlField;
    @FXML private TextField timeoutField;
    @FXML private TextField delayField;
    @FXML private TextField pollingField;

    @FXML private TableView<HeaderEntry> headersTable;
    @FXML private TableColumn<HeaderEntry, String> headerKeyColumn;
    @FXML private TableColumn<HeaderEntry, String> headerValueColumn;
    @FXML private TableColumn<HeaderEntry, Void> headerDeleteColumn;

    @FXML private Button startStopBtn;

    @FXML private Label currentStatusLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label avgTimeLabel;
    @FXML private Label okBadge;
    @FXML private Label errorBadge;
    @FXML private Label timeoutBadge;
    @FXML private Label progressTimeLabel;
    @FXML private ProgressBar sessionProgressBar;

    @FXML private TableView<PollResult> logTable;
    @FXML private TableColumn<PollResult, String> logTimeColumn;
    @FXML private TableColumn<PollResult, String> logStatusColumn;
    @FXML private TableColumn<PollResult, String> logResponseTimeColumn;
    @FXML private TableColumn<PollResult, String> logAvgResponseTimeColumn;

    private final ObservableList<HeaderEntry> headers = FXCollections.observableArrayList();
    private final ObservableList<PollResult> logItems = FXCollections.observableArrayList();
    private final LinkedList<Long> responseTimes = new LinkedList<>();

    private final PollerService pollerService = new PollerService();
    private Timeline sessionTimer;
    private int sessionElapsedSec;


    private int countOk;
    private int countError;
    private int countTimeout;

    @FXML
    public void initialize() {
        setupHeadersTable();
        setupLogTable();
        headersTable.setItems(headers);
        logTable.setItems(logItems);
        headersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        headers.addListener((javafx.collections.ListChangeListener<HeaderEntry>) change -> {
            headersTable.setPrefHeight(40 + headers.size() * 40 + 2);
        });
        headersTable.setPrefHeight(72);
    }

    private void setupHeadersTable() {
        headerKeyColumn.setCellValueFactory(cell -> cell.getValue().keyProperty());
        headerValueColumn.setCellValueFactory(cell -> cell.getValue().valueProperty());

        headerKeyColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        headerValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());

        headerKeyColumn.setOnEditCommit(event ->
                event.getRowValue().keyProperty().set(event.getNewValue()));

        headerValueColumn.setOnEditCommit(event ->
                event.getRowValue().valueProperty().set(event.getNewValue()));

        headerDeleteColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteBtn = new Button("x");

            {
                deleteBtn.setOnAction(event -> getTableView().getItems()
                        .remove(getIndex()));
                deleteBtn.getStyleClass().add("delete-button");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }

    private void setupLogTable() {
        logTimeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getFormattedTime()));
        logStatusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatusLabel()));
        logResponseTimeColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(String.format("%d ms", cell.getValue().getResponseMs())));
        logAvgResponseTimeColumn.setCellValueFactory(cell
                -> new SimpleStringProperty(String.format("%d ms", cell.getValue().getAvgResponseMs())));

        logTable.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(PollResult item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-ok", "row-error", "row-timeout");

                if (!empty && item != null) {
                    switch (item.getStatus()) {
                        case OK -> getStyleClass().add("row-ok");
                        case ERROR -> getStyleClass().add("row-error");
                        case TIMEOUT -> getStyleClass().add("row-timeout");
                    }
                }
            }
        });
    }

    @FXML
    private void onAddHeader() {
        headers.add(new HeaderEntry("", ""));
        headersTable.edit(headers.size() - 1, headerKeyColumn);
    }

    @FXML
    private void onReset() {
        stopPoller();
        urlField.clear();
        timeoutField.clear();
        delayField.clear();
        pollingField.clear();
        headers.clear();
        logItems.clear();
        responseTimes.clear();
        countOk = countError = countTimeout = 0;
        resetStatLabels();
    }

    @FXML
    private void onStartStop() {
        if (pollerService.isRunning()) {
            stopPoller();
        } else {
            startPoller();
        }
    }

    private void startPoller() {
        int sessionTotalSec;
        String url = urlField.getText().trim();
        if (url.isEmpty()) {
            showError("Enter URL");
            return;
        }

        int timeout;
        int delay;
        java.time.Duration pollingTime;

        try {
            timeout = Integer.parseInt(timeoutField.getText().trim());
            delay = Integer.parseInt(delayField.getText().trim());
            pollingTime = java.time.Duration.ofMinutes(Integer.parseInt(pollingField.getText().trim()));
        } catch (NumberFormatException _) {
            showError("Invalid timeout, delay, or polling interval");
            return;
        }

        Map<String, String> headerMap = headers.stream()
                .filter(entry -> !entry.getKey().isBlank())
                .collect(Collectors.toMap(HeaderEntry::getKey, HeaderEntry::getValue));

        pollerService.configure(
                url,
                timeout,
                delay,
                pollingTime,
                headerMap
        );

        sessionElapsedSec = 0;
        sessionTotalSec = (int) pollingTime.toSeconds();
        startSessionTimer(sessionTotalSec);

        pollerService.start(this::onPollResult, this::onSessionEnd);

        startStopBtn.setText("Stop");
        setConfigDisabled(true);
    }

    private void stopPoller() {
        pollerService.stop();
        if (sessionTimer != null) sessionTimer.stop();
        startStopBtn.setText("Run");
        setConfigDisabled(false);
    }

    private void startSessionTimer(int totalSec) {
        sessionTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            sessionElapsedSec++;
            int elapsed = sessionElapsedSec;
            progressTimeLabel.setText(formatTime(elapsed) + " / " + formatTime(totalSec));
            sessionProgressBar.setProgress((double) elapsed / totalSec);
        }));
        sessionTimer.setCycleCount(Animation.INDEFINITE);
        sessionTimer.play();
    }

    private void onSessionEnd() {
        stopPoller();
    }

    private void onPollResult(PollResult result) {
        switch (result.getStatus()) {
            case OK -> countOk++;
            case ERROR -> countError++;
            case TIMEOUT -> countTimeout++;
        }

        if (result.getStatus() != PollResult.Status.TIMEOUT) {
            responseTimes.addLast(result.getResponseMs());
            if (responseTimes.size() > 200) responseTimes.removeFirst();
        }

        long avg = responseTimes.isEmpty() ? 0
                : (long) responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        if (logItems.size() > 200) logItems.removeLast();
        result.setAvgResponseMs(avg);
        logItems.addFirst(result);

        String statusText = result.getStatusLabel();
        currentStatusLabel.setText(statusText);
        currentStatusLabel.getStyleClass().removeAll("status-ok", "status-error", "status-timeout");
        currentStatusLabel.getStyleClass().add(switch (result.getStatus()) {
            case OK -> "status-ok";
            case ERROR -> "status-error";
            case TIMEOUT -> "status-timeout";
        });

        currentTimeLabel.setText(String.format("%d ms", result.getResponseMs()));

        avgTimeLabel.setText(String.format("%d ms", avg));

        okBadge.setText(String.format("Success: %d", countOk));
        errorBadge.setText(String.format("Errors 4xx/5xx: %d", countError));
        timeoutBadge.setText(String.format("Timeouts: %d", countTimeout));
    }

    private void resetStatLabels() {
        currentStatusLabel.setText("—");
        currentTimeLabel.setText("—");
        avgTimeLabel.setText("—");
        okBadge.setText("Success: 0");
        errorBadge.setText("Errors 4xx/5xx: 0");
        timeoutBadge.setText("Timeouts: 0");
        sessionProgressBar.setProgress(0);
        progressTimeLabel.setText("0:00 / 0:00");
    }

    private void setConfigDisabled(boolean disabled) {
        urlField.setDisable(disabled);
        timeoutField.setDisable(disabled);
        delayField.setDisable(disabled);
        pollingField.setDisable(disabled);
        headersTable.setDisable(disabled);
    }

    private String formatTime(int totalSec) {
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
