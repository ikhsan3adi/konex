package org.konex.server.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.konex.common.interfaces.ChatRoom;
import org.konex.common.model.User;
import org.konex.server.database.DatabaseManager;
import org.konex.server.entity.GroupChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChatRoomService {
    private static final Logger LOGGER = Logger.getLogger(ChatRoomService.class.getName());
    private static ChatRoomService instance;

    // Map untuk menyimpan Room. Key: ChatID, Value: ChatRoom Object
    private final Map<String, ChatRoom> activeRooms = new ConcurrentHashMap<>();

    private ChatRoomService() {
        createGlobalRoom();

        loadGroupsFromDB();
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
            MongoCollection<Document> collection = DatabaseManager.getInstance().getCollection("groups");

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
                LOGGER.info("Group loaded from DB: " + groupName);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load groups: " + e.getMessage());
        }
    }

    public void saveGroup(GroupChat group) {
        try {
            List<String> memberPhones = group.getMembers().stream()
                    .map(User::getPhoneNumber)
                    .collect(Collectors.toList());

            Document doc = new Document()
                    .append("_id", group.getId())
                    .append("name", group.getName())
                    .append("adminPhone", group.getAdmin().getPhoneNumber())
                    .append("members", memberPhones);

            DatabaseManager.getInstance().getCollection("groups").updateOne(
                    Filters.eq("_id", group.getId()),
                    new Document("$set", doc),
                    new UpdateOptions().upsert(true)
            );

            activeRooms.put(group.getId(), group);

            LOGGER.info("Group saved to DB: " + group.getName());
        } catch (Exception e) {
            LOGGER.severe("Failed to save group: " + e.getMessage());
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
        } catch (Exception e) {
            // Ignore
        }
        return null;
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
}