package com.example.chatapp.findfriends;

public class FindFriendsModel {
    private String username;
    private String photoName;
    private String userId;
    private boolean requestSent;

    public FindFriendsModel(String username, String photoName, String userId, boolean requestSent) {
        this.username = username;
        this.photoName = photoName;
        this.userId = userId;
        this.requestSent = requestSent;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isRequestSent() {
        return requestSent;
    }

    public void setRequestSent(boolean requestSent) {
        this.requestSent = requestSent;
    }
}
