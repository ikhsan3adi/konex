package org.konex.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("login-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        stage.setTitle("KoneX - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main() {
        launch();
    }
}