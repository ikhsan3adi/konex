package org.konex.common.interfaces;

import org.konex.common.model.User;

public interface GroupManagement {
    /**
     * Mengundang member baru ke dalam grup.
     */
    void inviteMember(User u);

    /**
     * Mengeluarkan member dari grup.
     * Implementasi Proxy akan mengecek apakah requester adalah Admin.
     */
    void kickMember(User target, User requester);
}