package org.konex.common.model;

public class ImageMessage extends Message {
    private String base64Data;

    public ImageMessage() {
    }

    public ImageMessage(String chatId, User sender, String caption, String base64Data) {
        super(chatId, sender, caption);
        this.base64Data = base64Data;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public void setBase64Data(String base64Data) {
        this.base64Data = base64Data;
    }

    @Override
    public String getType() {
        return "IMAGE";
    }
}