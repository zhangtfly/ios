package com.example.myapplication.api.model;

/**
 * 登录请求
 */
public class LoginRequest {
    private String userAccount;
    private String userPassword;
    private Boolean rememberMe;

    public LoginRequest(String userAccount, String userPassword) {
        this.userAccount = userAccount;
        this.userPassword = userPassword;
        this.rememberMe = false;
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

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
