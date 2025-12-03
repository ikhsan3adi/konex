package org.konex.server.core;

import org.bson.Document;
import org.konex.common.model.ImageMessage;
import org.konex.common.model.Message;
import org.konex.common.model.TextMessage;
import org.konex.server.database.DatabaseManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private static final List<ClientHandler> CLIENTS = new CopyOnWriteArrayList<>();

    private final Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private volatile boolean running = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            CLIENTS.add(this);
            LOGGER.info(() -> "Client handler started for " + socket.getRemoteSocketAddress());
            listenLoop();
        } catch (EOFException eof) {
            LOGGER.info("Client disconnected cleanly");
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.WARNING, "Client handler error", e);
        } finally {
            shutdown();
        }
    }

    private void listenLoop() throws IOException, ClassNotFoundException {
        while (running && !socket.isClosed()) {
            Object payload = input.readObject();
            if (payload instanceof Message message) {
                broadcast(message);
            } else {
                LOGGER.warning("Received unknown payload type; ignoring");
            }
        }
    }

    private void broadcast(Message msg) {
        saveToDatabase(msg);

        for (ClientHandler client : CLIENTS) {
            client.send(msg);
        }
    }

    private void saveToDatabase(Message msg) {
        try {
            Document doc = new Document()
                    .append("chatId", msg.getChatId())
                    .append("senderPhone", msg.getSender().getPhoneNumber())
                    .append("senderName", msg.getSender().getName())
                    .append("timestamp", msg.getDate())
                    .append("type", msg.getType());

            if (msg instanceof TextMessage) {
                doc.append("content", msg.getContent());
            } else if (msg instanceof ImageMessage imgMsg) {
                doc.append("caption", msg.getContent());
                doc.append("base64Data", imgMsg.getBase64Data());
            }

            DatabaseManager.getInstance()
                    .getCollection("messages")
                    .insertOne(doc);

            LOGGER.info("Message saved to MongoDB: " + msg.getChatId());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save message to DB", e);
        }
    }

    private void send(Message msg) {
        try {
            synchronized (this) {
                if (output != null) {
                    output.writeObject(msg);
                    output.flush();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send message to client", e);
        }
    }

    private void shutdown() {
        running = false;
        CLIENTS.remove(this);
        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
        LOGGER.info(() -> "Client handler shutdown for " + socket.getRemoteSocketAddress());
    }

    private void closeQuietly(Object resource) {
        if (resource instanceof ObjectInputStream ois) {
            try {
                ois.close();
            } catch (IOException ignored) {
            }
        } else if (resource instanceof ObjectOutputStream oos) {
            try {
                oos.close();
            } catch (IOException ignored) {
            }
        } else if (resource instanceof Socket s) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }
}
