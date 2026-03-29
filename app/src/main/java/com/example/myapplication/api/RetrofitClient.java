package com.example.myapplication.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.X509TrustManager;

/**
 * Retrofit客户端
 */
public class RetrofitClient {
    private static final String BASE_URL = "http://222.19.82.142:8101/api/";
    private static RetrofitClient instance;
    private final ApiService apiService;

    private RetrofitClient() {
        // 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // 全局信任所有证书
        HttpsURLConnection.setDefaultSSLSocketFactory(SSLSocketClient.getSSLSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier(SSLSocketClient.getHostnameVerifier());
        // OkHttp客户端
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), (X509TrustManager) SSLSocketClient.getTrustManager()[0])
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                .build();

        // Retrofit实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}
