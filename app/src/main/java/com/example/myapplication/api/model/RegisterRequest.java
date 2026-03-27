package com.example.myapplication.api.model;

/**
 * 注册请求
 */
public class RegisterRequest {
    private String userName;
    private String userAccount;
    private String userPassword;
    private String userAvatar;
    private String userProfile;
    private String userRole;

    public RegisterRequest(String userName, String userAccount) {
        this.userName = userName;
        this.userAccount = userAccount;
        this.userRole = "user";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(String userAccount) {
        this.userAccount = userAccount;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}
