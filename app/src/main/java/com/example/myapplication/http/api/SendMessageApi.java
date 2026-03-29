package com.example.myapplication.http.api;

import com.hjq.http.config.IRequestApi;

public class SendMessageApi implements IRequestApi {

    @Override
    public String getApi() {
        return "api/v1/chat/completions";
    }
}
