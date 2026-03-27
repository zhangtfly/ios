package com.example.myapplication.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ACCOUNT = "user_account";
    private static final String KEY_USER_AVATAR = "user_avatar";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public void saveUserInfo(String token, Long userId, String userName, String userAccount, String userAvatar) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_ACCOUNT, userAccount)
                .putString(KEY_USER_AVATAR, userAvatar)
                .apply();
    }
    
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }
    
    public Long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1);
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    public String getUserAccount() {
        return prefs.getString(KEY_USER_ACCOUNT, null);
    }
    
    public String getUserAvatar() {
        return prefs.getString(KEY_USER_AVATAR, null);
    }
    
    public boolean isLoggedIn() {
        return getToken() != null;
    }
    
    public void clearUserInfo() {
        prefs.edit().clear().apply();
    }
}
