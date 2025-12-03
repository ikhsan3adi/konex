package org.konex.server.entity;

import org.konex.common.interfaces.GroupManagement;
import org.konex.common.model.User;

public class GroupProxy implements GroupManagement {
    @Override
    public void inviteMember(User u) {
        // TODO: Implement inviteMember
    }

    @Override
    public void kickMember(User target, User requester) {
        // TODO: Implement kickMember
    }
}
