package org.konex.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.konex.client.service.SocketClient;
import org.konex.common.interfaces.ChatObserver;
import org.konex.common.model.ImageMessage;
import org.konex.common.model.Message;
import org.konex.common.model.MessageFactory;
import org.konex.common.model.User;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class ChatController implements ChatObserver {

    @FXML
    private ListView<String> chatList;
    @FXML
    private VBox messageContainer;
    @FXML
    private TextArea messageInput;
    @FXML
    private Label headerLabel;
    @FXML
    private ScrollPane mainScrollPane;

    private SocketClient client;
    private User currentUser;

    public void initialize() {
        this.client = SocketClient.getInstance();
        this.currentUser = client.getCurrentUser();
        client.addObserver(this);

        headerLabel.setText("Global Chat Room");
        chatList.getItems().addAll("Global Chat");

        messageContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (mainScrollPane != null) mainScrollPane.setVvalue(1.0);
        });

        client.sendJoinMessage();
    }

    @FXML
    protected void onAttachImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Gambar");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        File file = fileChooser.showOpenDialog(messageInput.getScene().getWindow());

        if (file != null) {
            try {
                // Convert File ke Base64 String
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(fileContent);

                Message msg = MessageFactory.createMessage(
                        "global_room",
                        currentUser,
                        "[Gambar]",
                        base64Image
                );
                client.sendMessage(msg);

            } catch (IOException e) {
                showAlert("Error", "Gagal membaca file gambar.");
            }
        }
    }

    @FXML
    protected void onSendClick() {
        String text = messageInput.getText();
        if (text.isEmpty()) return;

        // Gunakan "global_room" atau ID statis dulu agar kode jalan
        String currentChatId = "global_room";
        Message msg = MessageFactory.createMessage(currentChatId, currentUser, text);
        client.sendMessage(msg);
        messageInput.clear();
    }

    @Override
    public void onNewMessage(Message msg) {
        Platform.runLater(() -> {
            if ("JOINED".equals(msg.getContent())) {
                addSystemLabel(msg.getSender().getName() + " bergabung ke chat.");
                return;
            }
            if ("LEFT".equals(msg.getContent())) {
                addSystemLabel(msg.getSender().getName() + " meninggalkan chat.");
                return;
            }

            boolean isSelf = msg.getSender().getPhoneNumber().equals(currentUser.getPhoneNumber());

            addBubbleChat(msg, isSelf);
        });
    }

    private void addSystemLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px; -fx-font-style: italic;");
        label.setPadding(new Insets(5, 0, 5, 0));
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);

        messageContainer.getChildren().add(label);
    }

    private void addBubbleChat(Message msg, boolean isSelf) {
        HBox row = new HBox();
        row.setPadding(new Insets(5));

        Node contentNode;

        if (msg instanceof ImageMessage imgMsg) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imgMsg.getBase64Data());
                Image img = new Image(new ByteArrayInputStream(imageBytes));
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                contentNode = imageView;
            } catch (Exception e) {
                contentNode = new Label("[Gambar Rusak]");
            }
        } else {
            String labelText = isSelf ? msg.getContent() : msg.getSender().getName() + ":\n" + msg.getContent();
            Label textLabel = new Label(labelText);
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(300);
            contentNode = textLabel;
        }


        HBox bubble = new HBox(contentNode);
        bubble.setPadding(new Insets(10));

        if (isSelf) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10;");
        }

        row.getChildren().add(bubble);
        messageContainer.getChildren().add(row);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}