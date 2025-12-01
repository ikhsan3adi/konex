package org.konex.common.model;

public class TextMessage extends Message {
    public TextMessage() {
    }

    public TextMessage(String chatId, User sender, String text) {
        super(chatId, sender, text);
    }

    @Override
    public String getType() {
        return "TEXT";
    }
}