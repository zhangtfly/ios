package com.example.myapplication.http.api;

import com.hjq.http.annotation.HttpIgnore;
import com.hjq.http.config.IRequestApi;

// 获取验证码
public class RegisterApi implements IRequestApi {
    @HttpIgnore
    private Boolean isRegister;

    public RegisterApi(boolean isRegisterType) {
        this.isRegister = isRegisterType;
    }

    @Override
    public String getApi() {
        return isRegister ? "api/user/register" : "api/user/resetPassword";
    }

    private String phoneNumber;
    private String verifyCode;
    private String newPassword;
    private String confirmPassword;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public RegisterApi setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public RegisterApi setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
        return this;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public RegisterApi setNewPassword(String newPassword) {
        this.newPassword = newPassword;
        return this;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public RegisterApi setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
        return this;
    }
}
