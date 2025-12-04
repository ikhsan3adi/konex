package org.konex.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.konex.client.ClientApp;
import org.konex.client.service.SocketClient;
import org.konex.common.constants.Constants;
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

    private String currentChatId = Constants.GLOBAL_ROOM_CHAT_ID;
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

        mainScrollPane.setStyle("-fx-background: #e5ddd5; -fx-background-color: #e5ddd5;");
        messageContainer.setStyle("-fx-background-color: #e5ddd5;");

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

        setupListViewStyle();
    }

    private void setupListViewStyle() {
        chatList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox cellLayout = new HBox();
                    cellLayout.setAlignment(Pos.CENTER_LEFT);
                    cellLayout.setSpacing(10);

                    String initial = !item.isEmpty() ? item.substring(0, 1).toUpperCase() : "#";
                    StackPane avatar = createGroupAvatar(initial);

                    Label nameLabel = new Label(item);
                    nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-font-weight: bold;");

                    cellLayout.getChildren().addAll(avatar, nameLabel);

                    setGraphic(cellLayout);
                    setText(null);

                    setPadding(new Insets(8, 10, 8, 10));

                    if (isSelected()) {
                        setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 5; -fx-cursor: hand;");
                    } else {
                        setStyle("-fx-background-color: white; -fx-border-color: #f0f0f0; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");
                    }
                }
            }
        });

        chatList.setStyle("-fx-background-color: transparent; -fx-control-inner-background: white; -fx-padding: 5;");
    }

    private StackPane createGroupAvatar(String letter) {
        Circle bg = new Circle(20);
        bg.setFill(Color.web("#2196F3"));

        Text text = new Text(letter);
        text.setFill(Color.web("#ffffff"));
        text.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        return new StackPane(bg, text);
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

    @Override
    public void onResponseReceived(Response<?> response) {
        Platform.runLater(() -> {
            String command = response.getCommand();

            if ("ROOMLIST".equals(response.getCommand()) && response.isSuccess()) {
                String rawPayload = (String) response.getData();
                updateSidebar(rawPayload);
            } else if ("NEW_MESSAGE".equals(response.getCommand()) && response.isSuccess()) {
                Message msg = (Message) response.getData();
                processIncomingMessage(msg);
            } else if ("KICKED".equals(command)) {
                String kickedChatId = (String) response.getData();
                handleKickedEvent(kickedChatId);
            } else if ("ERROR".equals(command)) {
                showAlert("Error", "Gagal: " + response.getMessage());
            } else if ("SYSTEM".equals(command)) {
                String payload = (String) response.getData();
                if (payload.startsWith("OPEN_PRIVATE:")) {
                    // Format: OPEN_PRIVATE:chatId:TargetName
                    String[] parts = payload.split(":");
                    String chatId = parts[1];
                    String chatName = parts[2];

                    joinRoom(chatId, chatName);

                    requestRoomList();
                }
            }
        });
    }

    private void processIncomingMessage(Message msg) {
        String content = msg.getContent();

        if (!msg.getChatId().equals(currentChatId)) {
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

    private void handleKickedEvent(String kickedChatId) {
        if (currentChatId.equals(kickedChatId)) {
            messageContainer.getChildren().clear();
            headerLabel.setText("Global Chat Room");
            currentChatId = "global_room";
            joinRoom("global_room", "Global Chat");

            showAlert("Info", "Anda telah dikeluarkan dari grup ini.");
        }

        requestRoomList();
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
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Kirim Gambar");
            dialog.setHeaderText("Tambahkan caption (opsional):");
            dialog.setContentText("Caption:");

            Optional<String> result = dialog.showAndWait();

            if (result.isEmpty()) return;

            String caption = result.get().trim();

            if (caption.isEmpty()) caption = "";

            try {
                byte[] fileContent = Files.readAllBytes(file.toPath());
                String base64Image = Base64.getEncoder().encodeToString(fileContent);

                Message msg = MessageFactory.createMessage(currentChatId, currentUser, caption, base64Image);
                client.sendMessage(msg);
            } catch (IOException _) {
                showAlert("Error", "Gagal membaca file gambar.");
            }
        }
    }

    @FXML
    protected void onLogoutClick() {
        try {
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
        row.setPadding(new Insets(5, 0, 5, 0));
        row.setSpacing(10);

        Node contentNode;
        if (msg instanceof ImageMessage imgMsg) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imgMsg.getBase64Data());
                Image img = new Image(new ByteArrayInputStream(imageBytes));
                ImageView imageView = new ImageView(img);
                imageView.setFitWidth(220);
                imageView.setPreserveRatio(true);
                imageView.setCursor(Cursor.HAND);

                imageView.imageProperty().addListener((obs, o, newImg) -> {
                    if (newImg != null) {
                        double h = newImg.getHeight() * (220 / newImg.getWidth());
                        javafx.scene.shape.Rectangle dynClip = new javafx.scene.shape.Rectangle(220, h);
                        dynClip.setArcWidth(15);
                        dynClip.setArcHeight(15);
                        imageView.setClip(dynClip);
                    }
                });

                imageView.setOnMouseClicked(event -> showFullscreenImage(img));

                String captionText = msg.getContent();
                if (captionText != null && !captionText.isEmpty()) {
                    Label captionLabel = new Label(captionText);
                    captionLabel.setWrapText(true);
                    captionLabel.setMaxWidth(220);
                    captionLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5 0 0 0;");
                    contentNode = new VBox(imageView, captionLabel);
                } else {
                    contentNode = imageView;
                }
            } catch (Exception _) {
                contentNode = new Label("⚠️ Gambar Rusak");
            }
        } else {
            Label textLabel = new Label(msg.getContent());
            textLabel.setWrapText(true);
            textLabel.setMaxWidth(300);
            textLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");
            contentNode = textLabel;
        }

        VBox bubble = new VBox();
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setMaxWidth(350);

        if (!isSelf) {
            Label nameLabel = new Label(msg.getSender().getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #E53935;"); // Merah bata
            nameLabel.setPadding(new Insets(0, 0, 3, 0));
            bubble.getChildren().add(nameLabel);
        }

        bubble.getChildren().add(contentNode);

        Label timeLabel = new Label(timeFormat.format(msg.getDate()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #757575;");
        timeLabel.setPadding(new Insets(4, 0, 0, 0));

        HBox timeBox = new HBox(timeLabel);
        timeBox.setAlignment(Pos.BOTTOM_RIGHT);
        bubble.getChildren().add(timeBox);

        if (isSelf) {
            row.setAlignment(Pos.CENTER_RIGHT);

            bubble.setStyle("-fx-background-color: #dcf8c6; " +
                    "-fx-background-radius: 15 15 0 15; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

            row.getChildren().add(bubble);

        } else {
            row.setAlignment(Pos.CENTER_LEFT);

            bubble.setStyle("-fx-background-color: #ffffff; " +
                    "-fx-background-radius: 15 15 15 0; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1);");

            Node avatarNode = createAvatar(msg.getSender());

            // Kirim Request Private Chat
            avatarNode.setCursor(Cursor.HAND);
            avatarNode.setOnMouseClicked(e -> {
                if (!msg.getSender().getPhoneNumber().equals(currentUser.getPhoneNumber())) {
                    // Kirim Request Private Chat
                    Message req = MessageFactory
                            .createMessage("SYSTEM", currentUser, "REQ_PRIVATE:" + msg.getSender().getPhoneNumber());
                    client.sendMessage(req);
                }
            });

            row.getChildren().addAll(avatarNode, bubble);
        }

        messageContainer.getChildren().add(row);
    }

    private Node createAvatar(User user) {
        double size = 35.0;

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            try {
                byte[] imgBytes = Base64.getDecoder().decode(user.getProfileImage());
                Image img = new Image(new ByteArrayInputStream(imgBytes));

                Circle circle = new Circle(size / 2);
                circle.setStroke(Color.LIGHTGRAY);
                circle.setFill(new javafx.scene.paint.ImagePattern(img));
                return circle;
            } catch (Exception _) {
                // do nothing
            }
        }

        Circle bg = new Circle(size / 2);
        bg.setFill(Color.web("#2196F3"));

        String initial = !user.getName().isEmpty() ? user.getName().substring(0, 1).toUpperCase() : "?";
        Text text = new Text(initial);
        text.setFill(Color.WHITE);
        text.setStyle("-fx-font-weight: bold;");

        return new StackPane(bg, text);
    }

    private void showFullscreenImage(Image image) {
        Stage stage = new Stage();
        stage.setTitle("Lihat Gambar");

        ImageView fullImageView = new ImageView(image);
        fullImageView.setPreserveRatio(true);
        fullImageView.setSmooth(true);
        fullImageView.setCache(true);

        StackPane root = new StackPane(fullImageView);
        root.setStyle("-fx-background-color: black;");
        root.setPadding(new Insets(20));

        fullImageView.fitWidthProperty().bind(root.widthProperty().subtract(40));
        fullImageView.fitHeightProperty().bind(root.heightProperty().subtract(40));

        Scene scene = new Scene(root, 800, 600);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        fullImageView.setOnMouseClicked(event -> stage.close());

        stage.setScene(scene);
        stage.show();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.show();
    }
}