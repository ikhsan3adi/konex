package org.konex.common.constants;

public class Constants {
    private Constants() {
    }

    public static final String GLOBAL_ROOM_CHAT_ID = "global_room";

    public static final String SYSTEM_SENDER = "SYSTEM";

    public static final String CMD_NEW_MESSAGE = "NEW_MESSAGE";
    public static final String CMD_ROOMLIST = "ROOMLIST";
    public static final String CMD_KICKED = "KICKED";
    public static final String CMD_ERROR = "ERROR";

    // Database field names
    public static final String FIELD_PHONE_NUMBER = "phoneNumber";
    public static final String FIELD_PROFILE_IMAGE = "profileImage";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_PASSWORD = "password";

    // Collection names
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_MESSAGES = "messages";
    public static final String COLLECTION_GROUPS = "groups";
}
