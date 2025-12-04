package org.konex.common.interfaces;

import org.konex.common.model.Response;

public interface ChatObserver {
    void onResponseReceived(Response<?> response);
}