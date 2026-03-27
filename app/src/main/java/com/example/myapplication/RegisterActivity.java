package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.api.BaseResponse;
import com.example.myapplication.api.RetrofitClient;
import com.example.myapplication.api.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUserName;
    private EditText etAccount;
    private EditText etPassword;
    private Button btnRegister;
    private TextView tvGoLogin;
    private ImageButton btnBack;
    private String originalRegisterButtonText; // 保存按钮原始文本

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String language = LanguageManager.getInstance(this).getLanguage();
        int layoutId = getLayoutByLanguage(language);
        setContentView(layoutId);

        // 添加返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupListeners();
    }
    
    private int getLayoutByLanguage(String language) {
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                return R.layout.activity_register_zh;
            case LanguageManager.LANGUAGE_ENGLISH:
                return R.layout.activity_register_en;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                return R.layout.activity_register;
        }
    }

    private void initViews() {
        etUserName = findViewById(R.id.et_user_name);
        etAccount = findViewById(R.id.et_account);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        tvGoLogin = findViewById(R.id.tv_go_login);
        btnBack = findViewById(R.id.btn_back);
        
        // 保存按钮原始文本（从布局中获取，已经是正确的语言）
        originalRegisterButtonText = btnRegister.getText().toString();
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> register());
        tvGoLogin.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }

    private void register() {
        String userName = etUserName.getText().toString().trim();
        String account = etAccount.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, getString(R.string.hint_username), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(account)) {
            Toast.makeText(this, getString(R.string.hint_account), Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText(getString(R.string.registering));

        RegisterRequest request = new RegisterRequest(userName, account);
        request.setUserPassword(password); 

        RetrofitClient.getInstance()
                .getApiService()
                .register(request)
                .enqueue(new Callback<BaseResponse<Long>>() {
                    @Override
                    public void onResponse(Call<BaseResponse<Long>> call, Response<BaseResponse<Long>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            BaseResponse<Long> baseResponse = response.body();
                            if (baseResponse.isSuccess()) {
                                Toast.makeText(RegisterActivity.this, getString(R.string.register_success), Toast.LENGTH_SHORT).show();
                                finish(); 
                            } else {
                                btnRegister.setEnabled(true);
                                btnRegister.setText(originalRegisterButtonText);
                                Toast.makeText(RegisterActivity.this, baseResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            btnRegister.setEnabled(true);
                            btnRegister.setText(originalRegisterButtonText);
                            Toast.makeText(RegisterActivity.this, getString(R.string.register_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<BaseResponse<Long>> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        btnRegister.setText(originalRegisterButtonText);
                        Toast.makeText(RegisterActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
