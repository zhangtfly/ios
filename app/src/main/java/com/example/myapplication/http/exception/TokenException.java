package com.example.myapplication.http.exception;

import com.hjq.http.exception.HttpException;

/**
 * ================================================
 * 作    者：ZJF-summoner
 * 版    本：1.0.0--改版
 * 创建日期：2023/5/9 9:52
 * 描    述：Token 失效异常
 * 修订历史：
 * ================================================
 */
public final class TokenException extends HttpException {

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}