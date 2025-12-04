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
import org.konex.client.ClientApp;
import org.konex.client.service.SocketClient;
import org.konex.common.interfaces.ChatObserver;
import org.konex.common.model.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    private String currentChatId = "global_room";
    private String currentChatName = "Global Chat";

    private final Map<String, String> roomMap = new HashMap<>();

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public void initialize() {
        this.client = SocketClient.getInstance();
        this.currentUser = client.getCurrentUser();
        client.addObserver(this);

        setupListView();

        messageContainer.heightProperty().addListener((_, _, _) -> {
            if (mainScrollPane != null) mainScrollPane.setVvalue(1.0);
        });

        joinRoom(currentChatId, currentChatName);
        requestRoomList();
    }

    private void setupListView() {
        chatList.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if (newValue != null) {
                String selectedRoomId = roomMap.get(newValue);
                if (selectedRoomId != null && !selectedRoomId.equals(currentChatId)) {
                    joinRoom(selectedRoomId, newValue);
                }
            }
        });
    }

    private void joinRoom(String chatId, String chatName) {
        this.currentChatId = chatId;
        this.currentChatName = chatName;

        Platform.runLater(() -> {
            headerLabel.setText(chatName);
            messageContainer.getChildren().clear();
        });

        Message joinMsg = MessageFactory.createMessage(chatId, currentUser, "JOINED");
        client.sendMessage(joinMsg);
    }

    private void requestRoomList() {
        Message reqMsg = MessageFactory.createMessage("SYSTEM", currentUser, "REQ_ROOMS");
        client.sendMessage(reqMsg);
    }

    @Deprecated
    @Override
    public void onNewMessage(Message msg) {

    }

    @Override
    public void onResponseReceived(Response<?> response) {
        Platform.runLater(() -> {
            System.out.println(response.toString());

            if ("ROOMLIST".equals(response.getCommand()) && response.isSuccess()) {
                String rawPayload = (String) response.getData();
                updateSidebar(rawPayload);
            } else if ("NEW_MESSAGE".equals(response.getCommand()) && response.isSuccess()) {
                Message msg = (Message) response.getData();
                processIncomingMessage(msg);
            }
        });
    }

    private void processIncomingMessage(Message msg) {
        String content = msg.getContent();

        if (!msg.getChatId().equals(currentChatId)) {
            // (Opsional: Logic notifikasi badge bisa ditaruh sini)
            return;
        }

        if ("JOINED".equals(content)) {
            addSystemLabel(msg.getSender().getName() + " bergabung.");
            return;
        }
        if ("LEFT".equals(content)) {
            addSystemLabel(msg.getSender().getName() + " keluar.");
            return;
        }

        boolean isSelf = msg.getSender().getPhoneNumber().equals(currentUser.getPhoneNumber());
        addBubbleChat(msg, isSelf);
    }

    private void updateSidebar(String rawData) {
        // Format data: "ROOMLIST:id1:name1,id2:name2"
        String payload = rawData.substring("ROOMLIST:".length());
        String[] rooms = payload.split(",");

        chatList.getItems().clear();
        roomMap.clear();

        for (String roomStr : rooms) {
            String[] parts = roomStr.split(":");
            if (parts.length >= 2) {
                String id = parts[0];
                String name = parts[1];

                roomMap.put(name, id);
                chatList.getItems().add(name);
            }
        }
    }

    @FXML
    protected void onCreateGroupClick() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Buat Grup Baru");
        dialog.setHeaderText("Silakan masukkan nama grup:");
        dialog.setContentText("Nama Grup:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                // Kirim perintah CREATE_GROUP ke server
                // Format: CREATE_GROUP:NamaGrup
                // ChatID "SYSTEM" dipakai karena ini pesan sistem
                Message msg = MessageFactory.createMessage("SYSTEM", currentUser, "CREATE_GROUP:" + name.trim());
                client.sendMessage(msg);
            }
        });
    }

    @FXML
    protected void onSendClick() {
        String text = messageInput.getText();
        if (text.isEmpty()) return;

        Message msg = MessageFactory.createMessage(currentChatId, currentUser, text);
        client.sendMessage(msg);
        messageInput.clear();
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
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(fileContent);

                Message msg = MessageFactory.createMessage(currentChatId, currentUser, "[Gambar]", base64Image);
                client.sendMessage(msg);
            } catch (IOException e) {
                showAlert("Error", "Gagal membaca file gambar.");
            }
        }
    }

    @FXML
    protected void onLogoutClick() {
        try {
            // SocketClient.getInstance().disconnect();

            javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(ClientApp.class.getResource("login-view.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load(), 400, 300);
            javafx.stage.Stage stage = (javafx.stage.Stage) headerLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addSystemLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
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
            String txt = isSelf ? msg.getContent() : msg.getSender().getName() + ":\n" + msg.getContent();
            contentNode = new Label(txt);
            ((Label) contentNode).setWrapText(true);
            ((Label) contentNode).setMaxWidth(300);
        }

        Label timeLabel = new Label(timeFormat.format(msg.getDate()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");
        timeLabel.setAlignment(Pos.BOTTOM_RIGHT);
        VBox bubbleContent = new VBox(contentNode, timeLabel);

        HBox bubble = new HBox(bubbleContent);
        bubble.setPadding(new Insets(10));
        if (isSelf) {
            row.setAlignment(Pos.CENTER_RIGHT);
            bubble.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            bubble.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #ddd;");
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