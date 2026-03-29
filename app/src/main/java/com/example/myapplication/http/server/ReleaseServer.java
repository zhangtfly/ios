package com.example.myapplication.http.server;

import androidx.annotation.NonNull;

import com.hjq.http.config.IHttpPostBodyStrategy;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.RequestBodyType;

public class ReleaseServer implements IRequestServer {

    @NonNull
    @Override
    public String getHost() {
//        return "http://222.19.82.142:8101/api/";
        return "https://derisively-nonchargeable-lailah.ngrok-free.dev/";
    }

    @NonNull
    @Override
    public IHttpPostBodyStrategy getBodyType() {
        return RequestBodyType.JSON;
    }
}