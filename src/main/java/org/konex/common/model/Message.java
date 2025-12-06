package org.konex.common.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Date;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextMessage.class, name = "TEXT"),
        @JsonSubTypes.Type(value = ImageMessage.class, name = "IMAGE")
})
public abstract class Message implements Serializable {
    protected String chatId;
    protected User sender;
    protected String content;
    protected Date date;

    protected Message() {
    }

    protected Message(String chatId, User sender, String content) {
        this.chatId = chatId;
        this.sender = sender;
        this.content = content;
        this.date = new Date();
    }

    public abstract String getType();

    // Getters
    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Date getDate() {
        return date;
    }

    public String getChatId() {
        return chatId;
    }

    // Setters
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}