package org.konex.common.interfaces;

import org.konex.common.model.Message;

public interface ChatRoom {
    /**
     * Mengirim pesan ke dalam room.
     * Implementasi di server akan mem-broadcast ke member lain.
     */
    void sendMessage(Message msg);

    /**
     * Mendapatkan ID unik dari room tersebut.
     */
    String getId();
}