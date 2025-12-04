package org.konex.server.core;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.konex.common.interfaces.ChatRoom;
import org.konex.common.model.*;
import org.konex.server.database.DatabaseManager;
import org.konex.server.entity.GroupChat;
import org.konex.server.entity.GroupProxy;
import org.konex.server.entity.PrivateChat;
import org.konex.server.service.ChatRoomService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    // SESSION MANAGER: Key = No HP, Value = ClientHandler
    private static final Map<String, ClientHandler> SESSIONS = new ConcurrentHashMap<>();

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

            LOGGER.info(() -> "Client connected: " + socket.getRemoteSocketAddress());

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
                String content = message.getContent();

                if (content != null && content.startsWith("AUTH_REQUEST:")) {
                    handleAuthRequest(message);
                } else if ("JOINED".equals(content)) {
                    handleJoin(message);
                } else if ("REQ_ROOMS".equals(content)) {
                    handleRoomRequest(message);
                } else if (content != null && content.startsWith("CREATE_GROUP:")) {
                    handleCreateGroup(message);
                } else if (content != null && content.startsWith("/kick")) {
                    handleKickCommand(message);
                } else {
                    routeMessage(message);
                }
            }
        }
    }

    private void handleAuthRequest(Message msg) {
        // format: "AUTH_REQUEST:password"
        String content = msg.getContent();
        String passwordInput = content.substring("AUTH_REQUEST:".length());

        User requestUser = msg.getSender();
        String phone = requestUser.getPhoneNumber();

        Document userDoc = DatabaseManager.getInstance().getCollection("users")
                .find(Filters.eq("phoneNumber", phone))
                .first();

        if (userDoc == null) {
            requestUser.setPassword(passwordInput);

            saveUserToDB(requestUser);

            this.currentUser = requestUser;
            SESSIONS.put(phone, this);

            LOGGER.info("New User Registered: " + requestUser.getName());
            sendResponse(Response.success("LOGIN_SUCCESS", requestUser));
        } else {
            String dbPassword = userDoc.getString("password");

            if (dbPassword != null && dbPassword.equals(passwordInput)) {
                User dbUser = new User();
                dbUser.setPhoneNumber(phone);
                dbUser.setName(userDoc.getString("name"));
                dbUser.setProfileImage(userDoc.getString("profileImage"));
                dbUser.setPassword(dbPassword);

                this.currentUser = dbUser;
                SESSIONS.put(phone, this);

                LOGGER.info("User Logged In: " + dbUser.getName());
                sendResponse(Response.success("LOGIN_SUCCESS", dbUser));
            } else {
                LOGGER.warning("Login Failed (Wrong Password): " + phone);
                sendResponse(Response.error("LOGIN_FAILED", "Password Salah!"));
            }
        }
    }

    private void handleJoin(Message msg) {
        this.currentUser = msg.getSender();

        SESSIONS.put(currentUser.getPhoneNumber(), this);

        saveUserToDB(currentUser);

        ChatRoom globalRoom = ChatRoomService.getInstance().getRoom(msg.getChatId());
        if (globalRoom instanceof GroupChat group) {
            group.inviteMember(currentUser);

            if (!group.getId().equals("global_room")) {
                ChatRoomService.getInstance().saveGroup(group);
            }

            LOGGER.info("User " + currentUser.getName() + " joined & saved to group: " + group.getName());
        }

        broadcastNotificationToAll(msg);

        loadAndSendHistory(msg.getChatId());

        LOGGER.info("User registered in session: " + currentUser.getName());
    }

    private void handleCreateGroup(Message msg) {
        // Format payload: "CREATE_GROUP:NamaGrup"
        String groupName = msg.getContent().substring("CREATE_GROUP:".length());

        if (groupName.isEmpty()) return;

        GroupChat newGroup = ChatRoomService.getInstance().createNewGroup(groupName, msg.getSender());

        LOGGER.info("New group created: " + groupName + " by " + msg.getSender().getName());

        broadcastRoomListUpdate();
    }

    private void handleRoomRequest(Message msg) {
        String payload = generateRoomListPayload();
        Response<String> response = Response.success("ROOMLIST", payload);

        sendResponse(response);
    }

    /**
     * Generate String: "ROOMLIST:id:name,id:name"
     */
    private String generateRoomListPayload() {
        StringBuilder sb = new StringBuilder("ROOMLIST:");
        Collection<ChatRoom> rooms = ChatRoomService.getInstance().getAllRooms();

        boolean first = true;
        for (ChatRoom room : rooms) {
            if (room instanceof GroupChat group) {
                if (!first) sb.append(",");
                sb.append(group.getId()).append(":").append(group.getName());
                first = false;
            }
        }
        return sb.toString();
    }

    private void broadcastRoomListUpdate() {
        String payload = generateRoomListPayload();
        Response<String> response = Response.success("ROOMLIST", payload);

        for (ClientHandler client : SESSIONS.values()) {
            client.sendResponse(response);
        }
    }

    private void routeMessage(Message msg) {
        saveToDatabase(msg);

        ChatRoom room = ChatRoomService.getInstance().getRoom(msg.getChatId());

        if (room == null) {
            LOGGER.warning("Room not found: " + msg.getChatId());
            return;
        }

        if (room instanceof GroupChat group) {
            if (!group.isMember(msg.getSender())) {
                Response<String> errorResp = Response.error("ERROR", "Anda bukan anggota grup ini.");
                sendResponse(errorResp);
                return;
            }
        }

        room.sendMessage(msg);

        if (room instanceof GroupChat group) {
            for (User member : group.getMembers()) {
                sendToTarget(member.getPhoneNumber(), msg);
            }
        } else if (room instanceof PrivateChat privateChat) {
            sendToTarget(privateChat.getFirstParticipant().getPhoneNumber(), msg);
            sendToTarget(privateChat.getSecondParticipant().getPhoneNumber(), msg);
        }
    }

    private void sendToTarget(String phoneNumber, Message msg) {
        ClientHandler targetClient = SESSIONS.get(phoneNumber);
        if (targetClient != null) {
            Response<Message> response = Response.success("NEW_MESSAGE", msg);
            targetClient.sendResponse(response);
        }
    }

    private void broadcastNotificationToAll(Message msg) {
        Response<Message> response = Response.success("NEW_MESSAGE", msg);

        for (ClientHandler client : SESSIONS.values()) {
            client.sendResponse(response);
        }
    }

    private void handleKickCommand(Message msg) {
        // Format perintah: "/kick <no hp>"
        String[] parts = msg.getContent().split(" ");

        if (parts.length < 2) {
            sendSystemMessageToClient("Format salah. Gunakan: /kick [NoHP]", msg.getSender());
            return;
        }

        String targetPhone = parts[1];
        String chatId = msg.getChatId();

        ChatRoom room = ChatRoomService.getInstance().getRoom(chatId);

        if (room instanceof GroupChat group) {
            // PROXY PATTERN
            GroupProxy proxy = new GroupProxy(group);

            User targetUser = new User();
            targetUser.setPhoneNumber(targetPhone);

            try {
                // cek msg.getSender() adalah Admin
                proxy.kickMember(targetUser, msg.getSender());

                ChatRoomService.getInstance().saveGroup(group);

                ClientHandler victimSession = SESSIONS.get(targetPhone);
                if (victimSession != null) {
                    Response<String> kickNotice = Response.success("KICKED", chatId);
                    victimSession.sendResponse(kickNotice);
                }

                sendSystemMessageToClient("Sukses mengeluarkan user: " + targetPhone, msg.getSender());

                Message announcement = new TextMessage(chatId, msg.getSender(), "telah mengeluarkan anggota " + targetPhone);
                routeMessage(announcement);

                LOGGER.info("KICK SUCCESS: " + targetPhone + " removed from " + group.getName() + " by " + msg.getSender().getName());

            } catch (SecurityException e) {
                LOGGER.warning("KICK FAILED: " + msg.getSender().getName() + " tried to kick but is not admin.");
                sendSystemMessageToClient("GAGAL: Anda bukan Admin grup ini!", msg.getSender());
            }
        } else {
            sendSystemMessageToClient("Perintah ini hanya berlaku di Grup.", msg.getSender());
        }
    }

    private void sendSystemMessageToClient(String text, User recipient) {
        User sysUser = new User();
        sysUser.setName("SYSTEM");
        sysUser.setPhoneNumber("0000");

        Message sysMsg = new TextMessage("SYSTEM_NOTIF", sysUser, text);

        Response<Message> response = Response.success("NEW_MESSAGE", sysMsg);

        this.sendResponse(response);
    }

    private void shutdown() {
        running = false;

        if (currentUser != null) {
            SESSIONS.remove(currentUser.getPhoneNumber());

            LOGGER.info(currentUser.getName() + " has left.");

            Message leftMsg = new TextMessage("global_room", currentUser, "LEFT");
            broadcastNotificationToAll(leftMsg);
        }

        closeQuietly(input);
        closeQuietly(output);
        closeQuietly(socket);
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

            DatabaseManager.getInstance().getCollection("messages").insertOne(doc);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "DB Error", e);
        }
    }

    private void saveUserToDB(User user) {
        try {
            Document doc = new Document()
                    .append("phoneNumber", user.getPhoneNumber())
                    .append("name", user.getName())
                    .append("password", user.getPassword())
                    .append("profileImage", user.getProfileImage());

            DatabaseManager.getInstance().getCollection("users").updateOne(
                    Filters.eq("phoneNumber", user.getPhoneNumber()),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
            );
        } catch (Exception e) {
            LOGGER.warning("Failed to save user: " + e.getMessage());
        }
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
                    Response<Message> response = Response.success("NEW_MESSAGE", msg);
                    this.sendResponse(response);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed load history", e);
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
            return null;
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
        } catch (IOException _) {
        }
    }

    private void sendResponse(Response<?> response) {
        try {
            synchronized (this) {
                if (output != null) {
                    output.writeObject(response);
                    output.flush();
                    output.reset();
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void closeQuietly(Object resource) {
        if (resource instanceof AutoCloseable c) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
    }
}