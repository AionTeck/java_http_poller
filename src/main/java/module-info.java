module org.chekhov.http_poller_ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires javafx.graphics;
    requires atlantafx.base;

    requires static lombok;
    requires java.naming;

    opens org.chekhov.http_poller_ui to javafx.fxml;
    opens org.chekhov.http_poller_ui.controller to javafx.fxml;
    opens org.chekhov.http_poller_ui.model to javafx.base;

    exports org.chekhov.http_poller_ui;
    exports org.chekhov.http_poller_ui.model;
    exports org.chekhov.http_poller_ui.controller;
    exports org.chekhov.http_poller_ui.service;
}