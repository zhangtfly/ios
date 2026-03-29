package com.example.myapplication.http.api;

import com.hjq.http.config.IRequestApi;

// 获取验证码
public class LoginPhoneV2Api implements IRequestApi {

    @Override
    public String getApi() {
        return "api/user/login/sms";
    }

    private String verifyCode;
    private String phoneNumber;
    private String rememberMe;

    public LoginPhoneV2Api setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
        return this;
    }

    public LoginPhoneV2Api setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public LoginPhoneV2Api setRememberMe(String rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }
}
