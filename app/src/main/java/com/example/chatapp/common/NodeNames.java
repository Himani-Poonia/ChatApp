package com.example.chatapp.common;

public class NodeNames {

    //Main nodes of database
    public static final String USERS = "users";
    public static final String FRIEND_REQUESTS = "FriendRequests";
    public static final String CHATS = "Chats";
    public static final String MESSAGES = "Messages";
    public static final String TOKEN = "Tokens";

    //children of "users" node
    public static final String NAME = "name";
    public static final String EMAIL = "email";
    public static final String ONLINE = "online";
    public static final String PHOTO = "photo";

    //children of "friendRequests" node
    public static final String REQUEST_TYPE = "request_type";

    //children of "messages" node
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE = "message";
    public static final String MESSAGE_TYPE = "messageType";
    public static final String MESSAGE_FROM = "messageFrom";
    public static final String MESSAGE_TIME = "messageTime";

    //child of "token" node
    public static final String DEVICE_TOKEN = "device_token";

    //children of "chats" node
    public static final String TIME_STAMP = "timestamp";
    public static final String UNREAD_COUNT = "unread_count";
    public static final String LAST_MESSAGE = "last_message";
    public static final String LAST_MESSAGE_TIME = "last_message_time";
    public static final String TYPING = "typing";
}
