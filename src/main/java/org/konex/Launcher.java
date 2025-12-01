package org.konex;

import javafx.application.Application;
import org.konex.client.ClientApp;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(ClientApp.class, args);
    }
}
