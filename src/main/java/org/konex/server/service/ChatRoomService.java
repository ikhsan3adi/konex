package org.konex.server.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.konex.common.constants.Constants;
import org.konex.common.interfaces.ChatRoom;
import org.konex.common.model.User;
import org.konex.server.database.DatabaseManager;
import org.konex.server.entity.GroupChat;
import org.konex.server.entity.PrivateChat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@SuppressWarnings("java:S6548")
public final class ChatRoomService {
    private static final Logger LOGGER = Logger.getLogger(ChatRoomService.class.getName());
    private static ChatRoomService instance;

    // Map untuk menyimpan Room. Key: ChatID, Value: ChatRoom Object
    private final Map<String, ChatRoom> activeRooms = new ConcurrentHashMap<>();

    private ChatRoomService() {
        createGlobalRoom();
        loadGroupsFromDB();
        loadPrivateChatsFromDB();
    }

    public static synchronized ChatRoomService getInstance() {
        if (instance == null) {
            instance = new ChatRoomService();
        }
        return instance;
    }

    private void createGlobalRoom() {
        User systemUser = new User();
        systemUser.setPhoneNumber("0000");
        systemUser.setName("System");
        GroupChat globalChat = new GroupChat("global_room", "Global Chat", systemUser);
        activeRooms.put(globalChat.getId(), globalChat);
    }

    private void loadGroupsFromDB() {
        try {
            MongoCollection<Document> collection = DatabaseManager.getInstance().getCollection(Constants.COLLECTION_GROUPS);

            for (Document doc : collection.find()) {
                String groupId = doc.getString("_id");
                if ("global_room".equals(groupId)) continue;

                String groupName = doc.getString("name");
                String adminPhone = doc.getString("adminPhone");
                List<String> memberPhones = doc.getList("members", String.class);

                User admin = findUserByPhone(adminPhone);
                if (admin == null) {
                    admin = new User();
                    admin.setPhoneNumber(adminPhone);
                    admin.setName("Unknown");
                }

                GroupChat group = new GroupChat(groupId, groupName, admin);

                if (memberPhones != null) {
                    for (String phone : memberPhones) {
                        User member = findUserByPhone(phone);
                        if (member != null) {
                            group.inviteMember(member);
                        }
                    }
                }

                activeRooms.put(groupId, group);
                LOGGER.info(() -> String.format("Group loaded from DB: %s", groupName));
            }
        } catch (Exception e) {
            LOGGER.warning(() -> String.format("Failed to load groups: %s", e.getMessage()));
        }
    }

    private void loadPrivateChatsFromDB() {
        try {
            MongoCollection<Document> collection = DatabaseManager.getInstance().getCollection(Constants.COLLECTION_GROUPS);
            for (Document doc : collection.find(Filters.eq("type", "PRIVATE"))) {
                String id = doc.getString("_id");
                String p1 = doc.getString("user1_phone");
                String p2 = doc.getString("user2_phone");

                User u1 = findUserByPhone(p1);
                User u2 = findUserByPhone(p2);

                if (u1 != null && u2 != null) {
                    PrivateChat pc = new PrivateChat(id, u1, u2);
                    activeRooms.put(id, pc);
                }
            }
        } catch (Exception _) {
            // Ignore
        }
    }

    public void saveGroup(GroupChat group) {
        try {
            List<String> memberPhones = group.getMembers().stream()
                    .map(User::getPhoneNumber)
                    .toList();

            Document doc = new Document()
                    .append("_id", group.getId())
                    .append("name", group.getName())
                    .append("adminPhone", group.getAdmin().getPhoneNumber())
                    .append("members", memberPhones);

            DatabaseManager.getInstance().getCollection(Constants.COLLECTION_GROUPS).updateOne(
                    Filters.eq("_id", group.getId()),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
            );

            activeRooms.put(group.getId(), group);

            LOGGER.info("Group saved to DB: " + group.getName());
        } catch (Exception e) {
            LOGGER.severe(() -> String.format("Failed to save group: %s", e.getMessage()));
        }
    }

    private User findUserByPhone(String phone) {
        try {
            Document doc = DatabaseManager.getInstance().getCollection("users")
                    .find(Filters.eq("phoneNumber", phone)).first();

            if (doc != null) {
                User u = new User();
                u.setPhoneNumber(doc.getString("phoneNumber"));
                u.setName(doc.getString("name"));
                u.setProfileImage(doc.getString("profileImage"));
                return u;
            }
        } catch (Exception _) {
            // Ignore
        }
        return null;
    }

    public java.util.Collection<ChatRoom> getAllRooms() {
        return activeRooms.values();
    }

    public ChatRoom getRoom(String id) {
        return activeRooms.get(id);
    }

    public GroupChat createNewGroup(String name, User admin) {
        String newId = "group_" + System.currentTimeMillis();
        GroupChat newGroup = new GroupChat(newId, name, admin);
        saveGroup(newGroup);
        return newGroup;
    }

    public ChatRoom getOrCreatePrivateChat(User user1, User user2) {
        String p1 = user1.getPhoneNumber();
        String p2 = user2.getPhoneNumber();

        String privateId;
        if (p1.compareTo(p2) < 0) {
            privateId = "private_" + p1 + "_" + p2;
        } else {
            privateId = "private_" + p2 + "_" + p1;
        }

        if (activeRooms.containsKey(privateId)) {
            return activeRooms.get(privateId);
        }

        PrivateChat newChat = new PrivateChat(privateId, user1, user2);

        savePrivateChat(newChat);
        activeRooms.put(privateId, newChat);

        return newChat;
    }

    public void savePrivateChat(org.konex.server.entity.PrivateChat chat) {
        try {
            Document doc = new Document()
                    .append("_id", chat.getId())
                    .append("type", "PRIVATE")
                    .append("user1_phone", chat.getFirstParticipant().getPhoneNumber())
                    .append("user2_phone", chat.getSecondParticipant().getPhoneNumber());

            DatabaseManager.getInstance().getCollection(Constants.COLLECTION_GROUPS).updateOne(
                    Filters.eq("_id", chat.getId()),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
            );

            LOGGER.info("Private Chat saved: " + chat.getId());
        } catch (Exception e) {
            LOGGER.severe(() -> String.format("Failed save private chat: %s", e.getMessage()));
        }
    }
}