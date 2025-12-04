package org.konex.common.interfaces;

import org.konex.common.model.Message;
import org.konex.common.model.Response;

public interface ChatObserver {
    /**
     * Method ini akan dipanggil otomatis ketika ada pesan baru masuk.
     * Di Client, ini akan update UI JavaFX.
     * Di Server, ini akan kirim JSON ke socket.
     */
    void onNewMessage(Message msg);

    void onResponseReceived(Response<?> response);
}