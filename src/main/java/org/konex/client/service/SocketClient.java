package org.konex.client.service;

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

    private SocketClient() {
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void connect(String host, int port, User user) throws IOException {
        this.currentUser = user;
        this.socket = new Socket(host, port);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.output.flush();
        this.input = new ObjectInputStream(socket.getInputStream());


        Thread listenerThread = new Thread(this::listen);
        listenerThread.setDaemon(true);
        listenerThread.start();
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
            System.err.println(e.getMessage());
        }
    }

    private void listen() {
        try {
            while (running && !socket.isClosed()) {
                Object data = input.readObject();
                if (data instanceof Message) {
                    notifyObservers((Message) data);
                } else if (data instanceof Response) {
                    notifyObservers((Response<?>) data);
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

    private void notifyObservers(Message msg) {
        for (ChatObserver observer : observers) {
            observer.onNewMessage(msg);
        }
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