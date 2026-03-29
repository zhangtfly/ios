package com.example.myapplication;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.myapplication.http.model.RequestHandler;
import com.example.myapplication.http.server.ReleaseServer;
import com.example.myapplication.utils.TokenManager;
import com.gyf.immersionbar.BuildConfig;
import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestInterceptor;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.HttpHeaders;
import com.hjq.http.model.HttpParams;
import com.hjq.http.request.HttpRequest;
import com.hjq.toast.Toaster;

import okhttp3.OkHttpClient;

public class AppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initSdk(this);
    }

    private void initSdk(AppApplication appApplication) {
        // 初始化 Toast 框架
        Toaster.init(this);


        // 网络请求框架初始化
        IRequestServer server = new ReleaseServer();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();

        EasyConfig.with(okHttpClient)
                // 是否打印日志
                .setLogEnabled(true)
                // 设置服务器配置（必须设置）
                .setServer(server)
                // 设置请求处理策略（必须设置）
                .setHandler(new RequestHandler(this))
                // 设置请求缓存实现策略（非必须）
//                .setCacheStrategy(new HttpCacheStrategy())
                // 设置请求参数拦截器
                .setInterceptor(new IRequestInterceptor() {
                    @Override
                    public void interceptArguments(@NonNull HttpRequest<?> httpRequest,
                                                   @NonNull HttpParams params,
                                                   @NonNull HttpHeaders headers) {
                        headers.put("timestamp", String.valueOf(System.currentTimeMillis()));
                        TokenManager tokenManager = new TokenManager(appApplication);
                        headers.put("Authorization",  "Bearer " + tokenManager.getToken());
                    }
                })
                // 设置请求重试次数
                .setRetryCount(1)
                // 设置请求重试时间
                .setRetryTime(2000)
                // 添加全局请求参数
//                .addParam("token", "6666666")
                // 添加全局请求头
                //.addHeader("date", "20191030")
                .into();
    }
}