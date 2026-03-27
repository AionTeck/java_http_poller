package org.chekhov.http_poller_ui;

import atlantafx.base.theme.NordDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class App extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Font.loadFont(App.class.getResourceAsStream("fonts/JetBrainsMono-Regular.ttf"), 13);
        Font.loadFont(App.class.getResourceAsStream("fonts/JetBrainsMono-Bold.ttf"), 13);


        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(
                App.class.getResource("main.fxml")
        );

        Scene scene = new Scene(loader.load(), 1000, 520);
        scene.getStylesheets().add(
                Objects.requireNonNull(App.class.getResource("styles.css")).toExternalForm()
        );

        stage.getIcons().add(
                new Image(
                        Objects.requireNonNull(
                                App.class.getResourceAsStream("icons/icon.png")
                        )
                )
        );

        stage.setTitle("HTTP Poller");
        stage.setMinWidth(900);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.show();
    }
}
