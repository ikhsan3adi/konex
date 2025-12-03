package org.konex.server.entity;

import org.konex.common.interfaces.ChatRoom;
import org.konex.common.interfaces.GroupManagement;
import org.konex.common.model.Message;
import org.konex.common.model.User;

public class GroupChat implements ChatRoom, GroupManagement {
    @Override
    public void sendMessage(Message msg) {
        // TODO: Implement sendMessage
    }

    @Override
    public String getId() {
        // TODO: Implement getId
        return "";
    }

    @Override
    public void inviteMember(User u) {
        // TODO: Implement inviteMember
    }

    @Override
    public void kickMember(User target, User requester) {
        // TODO: Implement kickMember
    }
}
