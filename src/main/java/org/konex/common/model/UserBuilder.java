package org.konex.common.model;

public class UserBuilder {
    private String userId;
    private String name;
    private String phoneNumber;
    private String profileImage;
    private String password;

    public UserBuilder setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public UserBuilder setPhone(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public UserBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public UserBuilder setProfileImage(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public UserBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public User build() {
        return new User(userId, name, phoneNumber, profileImage, password);
    }
}