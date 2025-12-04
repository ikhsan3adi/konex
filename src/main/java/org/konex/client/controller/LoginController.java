package org.konex.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.konex.client.ClientApp;
import org.konex.client.service.SocketClient;
import org.konex.common.model.User;
import org.konex.common.model.UserBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class LoginController {
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ImageView profilePreview;
    @FXML
    private Label uploadPlaceholder;

    private String base64ProfileImage = null;

    @FXML
    public void initialize() {
        Circle clip = new Circle(40, 40, 40);
        profilePreview.setClip(clip);
    }

    @FXML
    protected void onSelectPhotoClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Foto Profil");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        File file = fileChooser.showOpenDialog(nameField.getScene().getWindow());

        if (file != null) {
            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                this.base64ProfileImage = Base64.getEncoder().encodeToString(fileContent);

                Image img = new Image(new ByteArrayInputStream(fileContent));
                profilePreview.setImage(img);

                setImageCentered(profilePreview, img);

                if (uploadPlaceholder != null) {
                    uploadPlaceholder.setVisible(false);
                }
            } catch (IOException _) {
                showAlert("Error", "Gagal membaca file gambar.");
            }
        }
    }

    private void setImageCentered(ImageView imageView, Image img) {
        if (img == null) return;

        double w = img.getWidth();
        double h = img.getHeight();

        double minSize = Math.min(w, h);
        double x = (w - minSize) / 2;
        double y = (h - minSize) / 2;

        imageView.setViewport(new javafx.geometry.Rectangle2D(x, y, minSize, minSize));

        imageView.setImage(img);

        imageView.setFitWidth(80);
        imageView.setFitHeight(80);
        imageView.setSmooth(true);
    }

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
                    .setProfileImage(base64ProfileImage)
                    .build();

            SocketClient client = getSocketClient();

            client.sendAuthRequest(user);
        } catch (IOException _) {
            showAlert("Connection Failed", "Cannot connect to server.");
        }
    }

    private SocketClient getSocketClient() throws IOException {
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
        return client;
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