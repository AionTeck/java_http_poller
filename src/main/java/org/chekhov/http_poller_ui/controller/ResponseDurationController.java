package org.chekhov.http_poller_ui.controller;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;

import java.time.Duration;

public class ResponseDurationController {
    @FXML private LineChart<String, Number> responseDurationChart;
    @FXML private CategoryAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;
    @FXML private ScrollPane scrollPane;

    private final XYChart.Series<String, Number> chartSeries = new XYChart.Series<>();
    private long maxChartPoints = 200;

    private static final int VISIBLE_POINTS = 50;
    private static final int POINT_WIDTH = 20;

    private int pointCount = 0;

    @FXML
    public void initialize() {
        responseDurationChart.getData().add(chartSeries);

        chartYAxis.setTickLabelsVisible(true);
        chartYAxis.setTickMarkVisible(true);

        chartXAxis.setTickLabelsVisible(true);
        chartXAxis.setTickMarkVisible(true);

        chartXAxis.setTickLabelRotation(-45);
        chartXAxis.setGapStartAndEnd(false);
    }

    public void configure(Duration pollingMinutes, int delayMs) {
        maxChartPoints = pollingMinutes.toMillis() / delayMs;
        responseDurationChart.setPrefWidth(Math.max(700, maxChartPoints * POINT_WIDTH));
        clear();
    }

    public void addPoint(String time, long responseMs) {
        XYChart.Data<String, Number> point = new XYChart.Data<>(time, responseMs);
        chartSeries.getData().add(point);

        pointCount++;
        if (pointCount % 10 != 0) {
            point.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setVisible(false);
            });
        }

        if (chartSeries.getData().size() > maxChartPoints) {
            chartSeries.getData().removeFirst();
        }

        scrollEnd();
    }

    public void clear() {
        chartSeries.getData().clear();
    }

    private void scrollEnd() {
        if (scrollPane != null) {
            scrollPane.setHvalue(scrollPane.getHmax());
        }
    }
}
