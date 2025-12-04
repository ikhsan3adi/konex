package org.konex.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.konex.client.ClientApp;
import org.konex.client.service.SocketClient;
import org.konex.common.model.User;
import org.konex.common.model.UserBuilder;

import java.io.IOException;

public class LoginController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;

    @FXML
    protected void onLoginClick() {
        String name = nameField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();

        if (name.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Mohon isi semua data!");
            return;
        }

        try {
            User user = new UserBuilder()
                    .setName(name)
                    .setPhone(phone)
                    .setPassword(password)
                    .build();

            SocketClient client = SocketClient.getInstance();
            client.connect("localhost", 12345);

            client.setLoginCallback((success, message) -> {
                if (success) {
                    try {
                        loadChatView();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    showAlert("Login Gagal", message);
                    client.closeConnection();
                }
            });

            client.sendAuthRequest(user);
        } catch (IOException e) {
            showAlert("Connection Failed", "Cannot connect to server.");
        }
    }

    private void loadChatView() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ClientApp.class.getResource("chat-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.setTitle("KoneX - " + nameField.getText());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}