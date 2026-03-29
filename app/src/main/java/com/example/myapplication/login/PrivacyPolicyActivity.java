package com.example.myapplication.login;

import android.os.Bundle;
import android.widget.TextView;

import com.example.myapplication.R;
import com.example.myapplication.base.BaseActivity;
import com.hjq.bar.TitleBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.noties.markwon.Markwon;

public class PrivacyPolicyActivity extends BaseActivity {
    // 传递参数用的 KEY
    public static final String TYPE = "agreement_type";
    // 类型常量
    public static final int TYPE_PRIVACY = 1; // 隐私政策
    public static final int TYPE_USER = 2;    // 用户协议

    private int currentType = TYPE_PRIVACY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        // 获取传入的类型
        if (getIntent() != null) {
            currentType = getIntent().getIntExtra(TYPE, TYPE_PRIVACY);
        }

        // 初始化 Markwon 渲染
        Markwon markwon = Markwon.create(this);

        TextView contentTv = findViewById(R.id.tv_content);

        // 根据类型展示对应内容
        if (currentType == TYPE_USER) {
            setTitle("用户协议");
            markwon.setMarkdown(contentTv, readMarkdownFromAssets("user.md"));
        } else {
            setTitle("隐私政策");
            markwon.setMarkdown(contentTv, readMarkdownFromAssets("privacy.md"));
        }
    }
    /**
     * 从 assets 读取 .md 文件，UTF-8 编码，不乱码 + 保留所有样式
     */
    private String readMarkdownFromAssets(String fileName) {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, StandardCharsets.UTF_8)
             )) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "文件读取失败";
        }
        return stringBuilder.toString();
    }

    @Override
    protected boolean isStatusBarEnabled() {
        return true;
    }
}
