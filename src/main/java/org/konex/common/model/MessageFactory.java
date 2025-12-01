package org.konex.common.model;

import java.util.UUID;

public class MessageFactory {
    public static Message createMessage(User sender, String text) {
        return new TextMessage(UUID.randomUUID().toString(), sender, text);
    }

    public static Message createMessage(User sender, String caption, String base64Image) {
        return new ImageMessage(UUID.randomUUID().toString(), sender, caption, base64Image);
    }
}