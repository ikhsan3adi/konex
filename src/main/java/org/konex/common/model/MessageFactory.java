package org.konex.common.model;

public class MessageFactory {

    private MessageFactory() {
        //Utility class
    }

    public static Message createMessage(String chatId, User sender, String text) {
        return new TextMessage(chatId, sender, text);
    }

    public static Message createMessage(String chatId, User sender, String caption, String base64Image) {
        return new ImageMessage(chatId, sender, caption, base64Image);
    }
}