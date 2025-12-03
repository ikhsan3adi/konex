package org.konex.server.core;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.konex.common.model.ImageMessage;
import org.konex.common.model.Message;
import org.konex.common.model.TextMessage;
import org.konex.common.model.User;
import org.konex.server.database.DatabaseManager;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;
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

    private User currentUser;

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
                if ("JOINED".equals(message.getContent())) {
                    handleJoin(message);
                } else {
                    broadcast(message);
                }
            } else {
                LOGGER.warning("Received unknown payload type; ignoring");
            }
        }
    }

    private void broadcast(Message msg) {
        saveToDatabase(msg);
        broadcastNotification(msg);
    }

    private void broadcastNotification(Message msg) {
        for (ClientHandler client : CLIENTS) {
            client.send(msg);
        }
    }

    private void handleJoin(Message msg) {
        this.currentUser = msg.getSender();

        saveUserToDB(msg.getSender());

        broadcastNotification(msg);

        loadAndSendHistory("global_room");
    }

    private void loadAndSendHistory(String chatId) {
        try {
            FindIterable<Document> docs = DatabaseManager.getInstance()
                    .getCollection("messages")
                    .find(Filters.eq("chatId", chatId))
                    .sort(Sorts.ascending("timestamp"));

            for (Document doc : docs) {
                Message msg = documentToMessage(doc);
                if (msg != null) {
                    this.send(msg);
                }
            }
            LOGGER.info("History sent to client for chat: " + chatId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load history", e);
        }
    }

    private void saveUserToDB(User user) {
        try {
            Document doc = new Document()
                    .append("phoneNumber", user.getPhoneNumber())
                    .append("name", user.getName())
                    .append("profileImage", user.getProfileImage());

            // Upsert
            DatabaseManager.getInstance().getCollection("users").updateOne(
                    Filters.eq("phoneNumber", user.getPhoneNumber()),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
            );
            LOGGER.info("User saved/updated: " + user.getName());
        } catch (Exception e) {
            LOGGER.warning("Failed to save user: " + e.getMessage());
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
                    output.reset();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to send message to client", e);
        }
    }

    private Message documentToMessage(Document doc) {
        try {
            String type = doc.getString("type");
            String chatId = doc.getString("chatId");
            String senderPhone = doc.getString("senderPhone");
            String senderName = doc.getString("senderName");
            Date date = doc.getDate("timestamp");

            User sender = new User();
            sender.setPhoneNumber(senderPhone);
            sender.setName(senderName);

            Message msg;
            if ("TEXT".equals(type)) {
                String content = doc.getString("content");
                msg = new TextMessage(chatId, sender, content);
            } else if ("IMAGE".equals(type)) {
                String caption = doc.getString("caption");
                String base64 = doc.getString("base64Data");
                msg = new ImageMessage(chatId, sender, caption, base64);
            } else {
                return null;
            }
            msg.setDate(date);
            return msg;
        } catch (Exception e) {
            LOGGER.warning("Error parsing message document: " + e.getMessage());
            return null;
        }
    }

    private void shutdown() {
        running = false;
        CLIENTS.remove(this);

        if (currentUser != null) {
            LOGGER.info(currentUser.getName() + " has left the chat.");

            Message leftMsg = new TextMessage("global_room", currentUser, "LEFT");

            broadcastNotification(leftMsg);
        }

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
