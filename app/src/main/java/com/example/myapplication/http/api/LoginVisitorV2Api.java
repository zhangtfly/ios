package com.example.myapplication.http.api;

import com.hjq.http.config.IRequestApi;

// 获取验证码
public class LoginVisitorV2Api implements IRequestApi {

    @Override
    public String getApi() {
        return "api/user/login/guest";
    }
}
