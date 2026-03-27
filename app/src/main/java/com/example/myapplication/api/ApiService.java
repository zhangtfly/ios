package com.example.myapplication.api;

import com.example.myapplication.api.model.LoginRequest;
import com.example.myapplication.api.model.LoginResponse;
import com.example.myapplication.api.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * API接口定义
 */
public interface ApiService {
    
    /**
     * 用户登录
     */
    @POST("user/login")
    Call<BaseResponse<LoginResponse>> login(@Body LoginRequest request);
    
    /**
     * 用户注册
     */
    @POST("user/add")
    Call<BaseResponse<Long>> register(@Body RegisterRequest request);
}
