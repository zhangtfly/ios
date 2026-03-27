package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {
    private static final String PREF_NAME = "theme_pref";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_AUTO_SAVE_CHAT = "auto_save_chat";
    
    private static ThemeManager instance;
    private SharedPreferences preferences;
    
    private ThemeManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void setDarkMode(boolean isDarkMode) {
        preferences.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
        applyTheme(isDarkMode);
    }
    
    public boolean isDarkMode() {
        return preferences.getBoolean(KEY_DARK_MODE, false);
    }
    
    public void setAutoSaveChat(boolean autoSave) {
        preferences.edit().putBoolean(KEY_AUTO_SAVE_CHAT, autoSave).apply();
    }
    
    public boolean isAutoSaveChat() {
        return preferences.getBoolean(KEY_AUTO_SAVE_CHAT, true); 
    }
    
    public void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    public void initializeTheme() {
        
        if (!preferences.contains(KEY_DARK_MODE)) {
            setDarkMode(false);
        } else {
            applyTheme(isDarkMode());
        }
    }
}
