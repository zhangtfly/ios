package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class LanguageManager {
    private static final String PREF_NAME = "language_pref";
    private static final String KEY_LANGUAGE = "selected_language";
    
    public static final String LANGUAGE_TIBETAN = "bo";
    public static final String LANGUAGE_CHINESE = "zh";
    public static final String LANGUAGE_ENGLISH = "en";
    
    private static LanguageManager instance;
    private SharedPreferences preferences;
    
    private LanguageManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void setLanguage(String language) {
        preferences.edit().putString(KEY_LANGUAGE, language).apply();
    }
    
    public String getLanguage() {
        // 获取保存的语言，默认为藏文
        return preferences.getString(KEY_LANGUAGE, LANGUAGE_TIBETAN);
    }
    
    // 新增：获取初始语言（始终返回藏文）
    public String getInitialLanguage() {
        return LANGUAGE_TIBETAN;
    }
    
    public String getLanguageDisplayName(String language) {
        switch (language) {
            case LANGUAGE_TIBETAN:
                return "བོད་སྐད།";
            case LANGUAGE_CHINESE:
                return "中文";
            case LANGUAGE_ENGLISH:
                return "English";
            default:
                return "བོད་སྐད།";
        }
    }
    
    public interface OnLanguageChangeListener {
        void onLanguageChanged(String newLanguage);
    }
}
