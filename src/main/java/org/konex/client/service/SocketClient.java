package org.konex.client.service;

import javafx.application.Platform;
import org.konex.common.interfaces.ChatObserver;
import org.konex.common.model.Message;
import org.konex.common.model.MessageFactory;
import org.konex.common.model.Response;
import org.konex.common.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SocketClient {
    private static SocketClient instance;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private User currentUser;
    private volatile boolean running = true;
    private final List<ChatObserver> observers = new CopyOnWriteArrayList<>();

    private LoginCallback loginCallback;

    public interface LoginCallback {
        void onLoginResult(boolean success, String message);
    }

    private SocketClient() {
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) instance = new SocketClient();
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        if (socket != null && !socket.isClosed()) {
            closeConnection();
        }

        this.socket = new Socket(host, port);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.output.flush();
        this.input = new ObjectInputStream(socket.getInputStream());
        this.running = true;

        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void setLoginCallback(LoginCallback callback) {
        this.loginCallback = callback;
    }

    public void sendAuthRequest(User user) {
        this.currentUser = user;
        Message msg = MessageFactory.createMessage("SYSTEM", user, "AUTH_REQUEST:" + user.getPassword());
        sendMessage(msg);
    }

    public void sendJoinMessage() {
        if (currentUser == null) return;
        Message joinMsg = MessageFactory.createMessage("global_room", currentUser, "JOINED");
        sendMessage(joinMsg);
    }

    public void sendMessage(Message message) {
        try {
            synchronized (this) {
                if (output != null) {
                    output.writeObject(message);
                    output.flush();
                    output.reset();
                }
            }
        } catch (IOException e) {
            System.err.println("Send Error: " + e.getMessage());
        }
    }

    public void closeConnection() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
        }
    }

    private void listen() {
        try {
            while (running && !socket.isClosed()) {
                Object data = input.readObject();
                if (data instanceof Response) {
                    Response<?> resp = (Response<?>) data;
                    String cmd = resp.getCommand();

                    if ("LOGIN_SUCCESS".equals(cmd)) {
                        if (loginCallback != null) {
                            if (resp.getData() instanceof User) {
                                this.currentUser = (User) resp.getData();
                            }
                            Platform.runLater(() -> loginCallback.onLoginResult(true, "Login Sukses"));
                        }
                    } else if ("LOGIN_FAILED".equals(cmd)) {
                        if (loginCallback != null) {
                            Platform.runLater(() -> loginCallback.onLoginResult(false, resp.getMessage()));
                        }
                    } else {
                        notifyObservers(resp);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Disconnected.");
        }
    }

    public void addObserver(ChatObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ChatObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(Response<?> response) {
        for (ChatObserver observer : observers) {
            observer.onResponseReceived(response);
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }
}