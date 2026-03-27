package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    
    private ImageButton backButton;
    private LinearLayout accountSettingsOption;
    private LinearLayout darkModeOption;
    private SwitchCompat darkModeSwitch;
    private LinearLayout autoSaveChatOption;
    private SwitchCompat autoSaveChatSwitch;
    private LinearLayout aboutUsOption;
    private LinearLayout userAgreementOption;
    private LinearLayout privacyPolicyOption;
    
    private ThemeManager themeManager;
    private LanguageManager languageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        themeManager = ThemeManager.getInstance(this);
        themeManager.initializeTheme();
        
        super.onCreate(savedInstanceState);
        
        // 根据当前语言加载不同的布局
        languageManager = LanguageManager.getInstance(this);
        setContentView(getLayoutByLanguage());
        
        initViews();
        loadSettings();
        setupListeners();
    }
    
    private int getLayoutByLanguage() {
        String language = languageManager.getLanguage();
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                return R.layout.activity_settings_zh;
            case LanguageManager.LANGUAGE_ENGLISH:
                return R.layout.activity_settings_en;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                return R.layout.activity_settings_bo;
        }
    }
    
    private void initViews() {
        backButton = findViewById(R.id.backButton);
        accountSettingsOption = findViewById(R.id.accountSettingsOption);
        darkModeOption = findViewById(R.id.darkModeOption);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        autoSaveChatOption = findViewById(R.id.autoSaveChatOption);
        autoSaveChatSwitch = findViewById(R.id.autoSaveChatSwitch);
        aboutUsOption = findViewById(R.id.aboutUsOption);
        userAgreementOption = findViewById(R.id.userAgreementOption);
        privacyPolicyOption = findViewById(R.id.privacyPolicyOption);
    }
    
    private void setupListeners() {
        
        backButton.setOnClickListener(v -> {
            finish();
        });
        
        accountSettingsOption.setOnClickListener(v -> 
            showToast("账号设置功能开发中..."));
        
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setDarkMode(isChecked);
            
            showToast(isChecked ? "已切换到深色模式" : "已切换到浅色模式");
        
        });
        
        darkModeOption.setOnClickListener(v -> 
            darkModeSwitch.setChecked(!darkModeSwitch.isChecked()));
        
        autoSaveChatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themeManager.setAutoSaveChat(isChecked);
            String message = isChecked ? "已开启自动保存聊天记录" : "已关闭自动保存聊天记录";
            showToast(message);
        });
        
        autoSaveChatOption.setOnClickListener(v -> 
            autoSaveChatSwitch.setChecked(!autoSaveChatSwitch.isChecked()));
        
        aboutUsOption.setOnClickListener(v -> 
            showToast("关于我们功能开发中..."));
        
        userAgreementOption.setOnClickListener(v -> 
            showToast("用户协议功能开发中..."));
        
        privacyPolicyOption.setOnClickListener(v -> 
            showToast("隐私协议功能开发中..."));
    }
    
    private void loadSettings() {
        
        darkModeSwitch.setChecked(themeManager.isDarkMode());
        autoSaveChatSwitch.setChecked(themeManager.isAutoSaveChat());
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
