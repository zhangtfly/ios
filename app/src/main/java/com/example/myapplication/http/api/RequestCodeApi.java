package com.example.myapplication.http.api;

import com.hjq.http.config.IRequestApi;

// 获取验证码
public class RequestCodeApi implements IRequestApi {

    @Override
    public String getApi() {
        return "api/sms/send";
    }

    private String phoneNumber;


    public RequestCodeApi setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
}
