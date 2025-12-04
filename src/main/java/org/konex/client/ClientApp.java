package org.konex.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Mengarahkan ke login-view.fxml yang ada di resources/org/konex/client/
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("login-view.fxml"));

        // Ukuran window login (400x300 cukup proporsional)
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);

        stage.setTitle("KoneX - Login");
        stage.setScene(scene);
        stage.show();
    }

    public static void main() {
        launch();
    }
}