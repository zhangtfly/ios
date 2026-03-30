package com.example.myapplication;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

import com.example.myapplication.base.BaseActivity;
import com.example.myapplication.login.LoginV2Activity;
import com.gyf.immersionbar.ImmersionBar;

public class MainActivity extends BaseActivity implements LanguageManager.OnLanguageChangeListener {

    private static final int LOGIN_REQUEST_CODE = 1001;
    private LanguageManager languageManager;
    private boolean isLoadingHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeManager.getInstance(this).initializeTheme();

        super.onCreate(savedInstanceState);

        // 检查登录状态，未登录则跳转到登录页
        com.example.myapplication.utils.TokenManager tokenManager =
            new com.example.myapplication.utils.TokenManager(this);
        if (!tokenManager.isLoggedIn()) {
            Intent intent = new Intent(this, LoginV2Activity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        languageManager = LanguageManager.getInstance(this);

        // 确保初始语言是藏文
//        if (savedInstanceState == null) {
//            languageManager.setLanguage(LanguageManager.LANGUAGE_TIBETAN);
//        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, createChatFragment())
                    .commit();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_REQUEST_CODE) {
            // 登录页返回后，检查登录状态
            com.example.myapplication.utils.TokenManager tokenManager =
                new com.example.myapplication.utils.TokenManager(this);
            if (!tokenManager.isLoggedIn()) {
                // 未登录，退出应用
                finish();
                return;
            }
            // 已登录，重新初始化界面
            setContentView(R.layout.activity_main);
            languageManager = LanguageManager.getInstance(this);
//            languageManager.setLanguage(LanguageManager.LANGUAGE_TIBETAN);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, createChatFragment())
                    .commit();
        }
    }
    
    private ChatFragment createChatFragment() {
        ChatFragment chatFragment = new ChatFragment();
        
        Bundle args = new Bundle();
        args.putString("language", languageManager.getLanguage());
        chatFragment.setArguments(args);
        return chatFragment;
    }
    
    public void onLanguageChanged(String newLanguage) {
        
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        
        if (currentFragment instanceof ChatFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, createChatFragment())
                    .commit();
        } else if (currentFragment instanceof ProfileFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new ProfileFragment())
                    .commit();
        } else if (currentFragment instanceof HistoryFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HistoryFragment())
                    .commit();
        }
    }
    
    public void loadHistoryConversation(ChatHistory history) {getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        ChatFragment chatFragment = createChatFragment();
        Bundle args = chatFragment.getArguments();
        if (args == null) {
            args = new Bundle();
        }
        args.putString("history_id", history.getId());
        chatFragment.setArguments(args);getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, chatFragment)
                .addToBackStack(null)
                .commit();
        
        isLoadingHistory = true;}
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        
    }

    @NonNull
    @Override
    public ImmersionBar getStatusBarConfig() {
        return super.getStatusBarConfig().keyboardEnable(true);
    }
}