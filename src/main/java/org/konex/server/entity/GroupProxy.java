package org.konex.server.entity;

import org.konex.common.interfaces.GroupManagement;
import org.konex.common.model.User;

import java.util.Objects;

public class GroupProxy implements GroupManagement {
    private final GroupChat realGroup;

    public GroupProxy(GroupChat realGroup) {
        this.realGroup = Objects.requireNonNull(realGroup, "GroupChat cannot be null");
    }

    @Override
    public void inviteMember(User u) {
        realGroup.inviteMember(u);
    }

    @Override
    public void kickMember(User target, User requester) {
        if (!realGroup.isAdmin(requester)) {
            throw new SecurityException("Only admin can remove members from this group");
        }
        realGroup.kickMember(target, requester);
    }

    public GroupChat unwrap() {
        return realGroup;
    }
}
