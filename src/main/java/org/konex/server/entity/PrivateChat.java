package org.konex.server.entity;

import org.konex.common.interfaces.ChatRoom;
import org.konex.common.model.Message;
import org.konex.common.model.User;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class PrivateChat implements ChatRoom {
    private static final Logger LOGGER = Logger.getLogger(PrivateChat.class.getName());

    private final String id;
    private final User userA;
    private final User userB;
    private final List<Message> history = new CopyOnWriteArrayList<>();

    public PrivateChat(String id, User userA, User userB) {
        if (id == null || userA == null || userB == null) {
            throw new IllegalArgumentException("Private chat requires non-null id and participants");
        }
        this.id = id;
        this.userA = userA;
        this.userB = userB;
    }

    @Override
    public void sendMessage(Message msg) {
        Objects.requireNonNull(msg, "Message cannot be null");
        User recipient = resolveRecipient(msg.getSender());
        if (recipient == null) {
            LOGGER.warning("Sender is not part of this private chat");
            return;
        }
        history.add(msg);
        deliver(recipient, msg);
    }

    private void deliver(User recipient, Message msg) {
        LOGGER.info(() -> String.format(
                "Delivering private message from %s to %s in chat %s",
                safeName(msg.getSender()),
                safeName(recipient),
                id));
    }

    private User resolveRecipient(User sender) {
        if (matches(userA, sender)) {
            return userB;
        }
        if (matches(userB, sender)) {
            return userA;
        }
        return null;
    }

    private boolean matches(User expected, User actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return Objects.equals(expected.getUserId(), actual.getUserId());
    }

    private String safeName(User user) {
        return user == null ? "unknown" : user.getName();
    }

    @Override
    public String getId() {
        return id;
    }

    public List<Message> getHistory() {
        return List.copyOf(history);
    }

    public User getFirstParticipant() {
        return userA;
    }

    public User getSecondParticipant() {
        return userB;
    }
}
