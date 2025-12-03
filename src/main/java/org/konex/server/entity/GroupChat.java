package org.konex.server.entity;

import org.konex.common.interfaces.ChatRoom;
import org.konex.common.interfaces.GroupManagement;
import org.konex.common.model.Message;
import org.konex.common.model.User;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class GroupChat implements ChatRoom, GroupManagement {
    private static final Logger LOGGER = Logger.getLogger(GroupChat.class.getName());

    private final String id;
    private final String name;
    private final User admin;
    private final List<User> members = new CopyOnWriteArrayList<>();
    private final List<Message> history = new CopyOnWriteArrayList<>();

    public GroupChat(String id, String name, User admin) {
        if (id == null || name == null || admin == null) {
            throw new IllegalArgumentException("Group chat requires id, name, and admin");
        }
        this.id = id;
        this.name = name;
        this.admin = admin;
        members.add(admin);
    }

    @Override
    public void sendMessage(Message msg) {
        Objects.requireNonNull(msg, "Message cannot be null");
        history.add(msg);
        for (User member : members) {
            deliver(member, msg);
        }
    }

    private void deliver(User member, Message msg) {
        LOGGER.info(() -> String.format(
                "Broadcasting message in group %s from %s to %s",
                name,
                safeName(msg.getSender()),
                safeName(member)));
    }

    private String safeName(User user) {
        return user == null ? "unknown" : user.getName();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getAdmin() {
        return admin;
    }

    public List<User> getMembers() {
        return List.copyOf(members);
    }

    public List<Message> getHistory() {
        return List.copyOf(history);
    }

    @Override
    public void inviteMember(User u) {
        Objects.requireNonNull(u, "User cannot be null");
        if (members.stream().noneMatch(member -> sameUser(member, u))) {
            members.add(u);
            LOGGER.info(() -> String.format("%s joined group %s", safeName(u), name));
        }
    }

    @Override
    public void kickMember(User target, User requester) {
        if (target == null) {
            return;
        }
        boolean removed = members.removeIf(member -> sameUser(member, target));
        if (removed) {
            LOGGER.info(() -> String.format("%s removed from group %s by %s",
                    safeName(target), name, safeName(requester)));
        }
    }

    public boolean isAdmin(User user) {
        return sameUser(admin, user);
    }

    public boolean isMember(User user) {
        return user != null && members.stream().anyMatch(member -> sameUser(member, user));
    }

    private boolean sameUser(User a, User b) {
        if (a == null || b == null) {
            return false;
        }
        return Objects.equals(a.getUserId(), b.getUserId());
    }
}
