package org.konex.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.konex.client.service.SocketClient;
import org.konex.common.interfaces.ChatObserver;
import org.konex.common.model.Message;
import org.konex.common.model.MessageFactory;
import org.konex.common.model.User;

public class ChatController implements ChatObserver {

    @FXML private ListView<String> chatList;
    @FXML private VBox messageContainer;
    @FXML private TextArea messageInput;
    @FXML private Label headerLabel;

    private SocketClient client;
    private User currentUser;

    public void initialize() {
        this.client = SocketClient.getInstance();
        this.currentUser = client.getCurrentUser();
        client.addObserver(this);
        
        headerLabel.setText("Global Chat Room");
        chatList.getItems().addAll("Global Chat");
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
        addBubbleChat(msg, true);
    }

    @Override
    public void onNewMessage(Message msg) {
        Platform.runLater(() -> {
            boolean isSelf = msg.getSender().getPhoneNumber().equals(currentUser.getPhoneNumber());
            if (!isSelf) {
                addBubbleChat(msg, false);
            }
        });
    }

    private void addBubbleChat(Message msg, boolean isSelf) {
        HBox row = new HBox();
        row.setPadding(new Insets(5));
        
        Label content = new Label(msg.getSender().getName() + ":\n" + msg.getContent());
        content.setWrapText(true);
        content.setMaxWidth(300);
        content.setPadding(new Insets(10));
        
        if (isSelf) {
            row.setAlignment(Pos.CENTER_RIGHT);
            content.setStyle("-fx-background-color: #DCF8C6; -fx-background-radius: 10; -fx-font-size: 14px;");
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            content.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-border-color: #E0E0E0; -fx-border-radius: 10;");
        }
        
        row.getChildren().add(content);
        messageContainer.getChildren().add(row);
    }
}