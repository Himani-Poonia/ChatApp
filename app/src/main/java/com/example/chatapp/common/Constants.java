package com.example.chatapp.common;

public class Constants {
    public static final String IMAGES_FOLDER = "images";
    public static final String MESSAGE_VIDEOS = "message_videos";
    public static final String MESSAGE_IMAGES = "message_images";

    public static final String REQUEST_DATA_SENT = "sent";
    public static final String REQUEST_DATA_RECEIVED = "received";
    public static final String REQUEST_DATA_ACCEPTED = "accepted";

    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_VIDEO = "video";

    public static final String NOTIFICATION_TITLE = "title";
    public static final String NOTIFICATION_MESSAGE = "message";

    public static final String NOTIFICATION_DATA = "data";
    public static final String NOTIFICATION_TO = "to";

    //fcm
    public static final String FIREBASE_KEY = ""; //enter your firebase key here
    public static final String SENDER_ID = ""; //enter your SENDER_ID here

    //notification channel (to receive notification)
    public static final String CHANNEL_ID = "chat_app_01";
    public static final String CHANNEL_NAME = "chat_app_notifications";
    public static final String CHANNEL_DESC = "Chat App notifications";

    //user status
    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";
    public static final String STATUS_TYPING = "typing...";

    public static final String TYPING_STOPPED = "0";
    public static final String TYPING_STARTED = "1";
}