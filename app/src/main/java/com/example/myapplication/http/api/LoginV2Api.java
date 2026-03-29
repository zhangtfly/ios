package com.example.myapplication.http.api;

import com.hjq.http.config.IRequestApi;

// 获取验证码
public class LoginV2Api implements IRequestApi {

    @Override
    public String getApi() {
        return "api/user/login";
    }

    private String userAccount;
    private String userPassword;
    private String rememberMe;

    public String getUserAccount() {
        return userAccount;
    }

    public LoginV2Api setUserAccount(String userAccount) {
        this.userAccount = userAccount;
        return this;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public LoginV2Api setUserPassword(String userPassword) {
        this.userPassword = userPassword;
        return this;
    }

    public String getRememberMe() {
        return rememberMe;
    }

    public LoginV2Api setRememberMe(String rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }
}
