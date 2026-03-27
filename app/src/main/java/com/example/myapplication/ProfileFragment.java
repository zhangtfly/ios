package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {
    
    private LanguageManager languageManager;
    private TextView currentLanguageText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        languageManager = LanguageManager.getInstance(getContext());
        
        View view = loadLayoutByLanguage(inflater, container);
        
        setupClickListeners(view);
        
        updateLanguageDisplay(view);
        
        TibetanFontHelper.applyTibetanFontToView(getContext(), view);
        
        setupKeyboardDismissal(view);
        
        return view;
    }
    
    private View loadLayoutByLanguage(LayoutInflater inflater, ViewGroup container) {
        String currentLanguage = languageManager.getLanguage();
        int layoutId;
        
        switch (currentLanguage) {
            case LanguageManager.LANGUAGE_CHINESE:
                layoutId = R.layout.fragment_profile; 
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                layoutId = R.layout.fragment_profile_en; 
                break;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                layoutId = R.layout.fragment_profile_bo; 
                break;
        }
        
        return inflater.inflate(layoutId, container, false);
    }
    
    private void updateLanguageDisplay(View view) {
        currentLanguageText = view.findViewById(R.id.currentLanguage);
        if (currentLanguageText != null) {
            String currentLanguage = languageManager.getLanguage();
            currentLanguageText.setText(languageManager.getLanguageDisplayName(currentLanguage));
        }
    }
    
    private void setupClickListeners(View view) {
        
        view.findViewById(R.id.settingsOption).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            startActivity(intent);
        });
        
        view.findViewById(R.id.languageOption).setOnClickListener(v -> 
            showLanguageDialog());
            
        view.findViewById(R.id.upgradeOption).setOnClickListener(v -> 
            showToast("升级计划功能开发中..."));
            
        view.findViewById(R.id.learnMoreOption).setOnClickListener(v -> 
            showToast("了解更多功能开发中..."));
            
        view.findViewById(R.id.helpOption).setOnClickListener(v -> 
            showToast("获取帮助功能开发中..."));
            
        view.findViewById(R.id.logoutOption).setOnClickListener(v -> 
            showToast("退出登录功能开发中..."));
    }
    
    private void showLanguageDialog() {
        String[] languages = {"བོད་སྐད།", "中文", "English"};
        String[] languageCodes = {
            LanguageManager.LANGUAGE_TIBETAN,
            LanguageManager.LANGUAGE_CHINESE,
            LanguageManager.LANGUAGE_ENGLISH
        };
        
        String currentLanguage = languageManager.getLanguage();
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                selectedIndex = i;
                break;
            }
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle("选择语言 / སྐད་ཡིག་འདེམས། / Select Language")
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    String newLanguage = languageCodes[which];
                    languageManager.setLanguage(newLanguage);
                    
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).onLanguageChanged(newLanguage);
                    }
                    
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void setupKeyboardDismissal(View view) {
        
        if (view instanceof ViewGroup) {
            view.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    hideKeyboard();
                }
                return false;
            });
        }
    }
    
    private void hideKeyboard() {
        if (getActivity() != null && getView() != null) {
            android.view.inputmethod.InputMethodManager imm = 
                (android.view.inputmethod.InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }
}
