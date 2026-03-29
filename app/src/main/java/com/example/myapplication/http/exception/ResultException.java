package com.example.myapplication.http.exception;

import androidx.annotation.NonNull;

import com.example.myapplication.http.model.HttpData;
import com.hjq.http.exception.HttpException;

/**
 * ================================================
 * 作    者：ZJF-summoner
 * 版    本：1.0.0--改版
 * 创建日期：2023/5/9 9:52
 * 描    述：返回结果异常
 * 修订历史：
 * ================================================
 */
public final class ResultException extends HttpException {

    private final HttpData<?> mData;

    public ResultException(String message, HttpData<?> data) {
        super(message);
        mData = data;
    }

    public ResultException(String message, Throwable cause, HttpData<?> data) {
        super(message, cause);
        mData = data;
    }

    @NonNull
    public HttpData<?> getHttpData() {
        return mData;
    }
}