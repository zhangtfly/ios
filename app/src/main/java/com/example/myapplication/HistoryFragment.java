package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryFragment extends Fragment implements ChatHistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView historyRecyclerView;
    private LinearLayout emptyStateLayout;
    private LinearLayout searchContainer;
    private EditText searchEditText;
    private ImageButton searchButton;
    private ImageButton closeSearchButton;
    private ImageButton clearAllButton;
    
    private ChatHistoryAdapter adapter;
    private ChatHistoryManager historyManager;
    private boolean isSearchMode = false;
    private LanguageManager languageManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        languageManager = LanguageManager.getInstance(getContext());
        return inflater.inflate(getLayoutByLanguage(), container, false);
    }
    
    private int getLayoutByLanguage() {
        String language = languageManager.getLanguage();
        switch (language) {
            case LanguageManager.LANGUAGE_CHINESE:
                return R.layout.fragment_history;
            case LanguageManager.LANGUAGE_ENGLISH:
                return R.layout.fragment_history_en;
            case LanguageManager.LANGUAGE_TIBETAN:
            default:
                return R.layout.fragment_history_bo;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadHistoryData();
        
        TibetanFontHelper.applyTibetanFontToView(getContext(), view);
        
        setupKeyboardDismissal(view);
    }

    private void initViews(View view) {
        historyRecyclerView = view.findViewById(R.id.historyRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchContainer = view.findViewById(R.id.searchContainer);
        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        closeSearchButton = view.findViewById(R.id.closeSearchButton);
        clearAllButton = view.findViewById(R.id.clearAllButton);
        
        ImageButton backButton = view.findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);}
            });
        }
        
        historyManager = ChatHistoryManager.getInstance(getContext());
    }

    private void setupRecyclerView() {
        adapter = new ChatHistoryAdapter();
        adapter.setOnHistoryItemClickListener(this);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        
        searchButton.setOnClickListener(v -> toggleSearchMode());
        
        closeSearchButton.setOnClickListener(v -> closeSearchMode());
        
        clearAllButton.setOnClickListener(v -> showClearAllDialog());
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadHistoryData() {
        List<ChatHistory> historyList = historyManager.getAllHistory();
        adapter.updateHistoryList(historyList);
        updateEmptyState(historyList.isEmpty());
    }

    private void performSearch(String keyword) {
        List<ChatHistory> searchResults = historyManager.searchHistory(keyword);
        adapter.updateHistoryList(searchResults);
        updateEmptyState(searchResults.isEmpty());
    }

    private void toggleSearchMode() {
        isSearchMode = !isSearchMode;
        if (isSearchMode) {
            searchContainer.setVisibility(View.VISIBLE);
            searchEditText.requestFocus();
        } else {
            closeSearchMode();
        }
    }

    private void closeSearchMode() {
        isSearchMode = false;
        searchContainer.setVisibility(View.GONE);
        searchEditText.setText("");
        loadHistoryData(); 
    }

    private void showClearAllDialog() {
        String language = languageManager.getLanguage();
        String title, message, positive, negative, toastMsg;
        
        switch (language) {
            case LanguageManager.LANGUAGE_TIBETAN:
                title = "ལོ་རྒྱུས་སྤང་བ།";
                message = "ལོ་རྒྱུས་ཡོངས་རྫོགས་སྤང་རྒྱུ་གཏན་འཁེལ་ལམ། འདི་བྱས་ན་སླར་གསོ་བྱེད་ཐབས་མེད།";
                positive = "གཏན་འཁེལ།";
                negative = "མི་འདོད།";
                toastMsg = "ལོ་རྒྱུས་སྤངས་ཟིན།";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                title = "Clear History";
                message = "Are you sure you want to clear all history? This action cannot be undone.";
                positive = "Confirm";
                negative = "Cancel";
                toastMsg = "History cleared";
                break;
            case LanguageManager.LANGUAGE_CHINESE:
            default:
                title = "清空历史记录";
                message = "确定要清空所有历史记录吗？此操作不可恢复。";
                positive = "确定";
                negative = "取消";
                toastMsg = "历史记录已清空";
                break;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, (dialog, which) -> {
                    historyManager.clearAllHistory();
                    loadHistoryData();
                    safeShowToast(toastMsg);
                })
                .setNegativeButton(negative, null)
                .show();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            historyRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            historyRecyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onHistoryClick(ChatHistory history) {if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();mainActivity.loadHistoryConversation(history);
        } else {}
    }

    @Override
    public void onHistoryDelete(ChatHistory history) {
        String language = languageManager.getLanguage();
        String title, message, positive, negative, toastMsg;
        
        switch (language) {
            case LanguageManager.LANGUAGE_TIBETAN:
                title = "གླེང་མོལ་སུབ་པ།";
                message = "གླེང་མོལ་འདི་སུབ་རྒྱུ་གཏན་འཁེལ་ལམ།";
                positive = "སུབ་པ།";
                negative = "མི་འདོད།";
                toastMsg = "གླེང་མོལ་སུབས་ཟིན།";
                break;
            case LanguageManager.LANGUAGE_ENGLISH:
                title = "Delete Conversation";
                message = "Are you sure you want to delete this conversation?";
                positive = "Delete";
                negative = "Cancel";
                toastMsg = "Conversation deleted";
                break;
            case LanguageManager.LANGUAGE_CHINESE:
            default:
                title = "删除对话";
                message = "确定要删除这条对话记录吗？";
                positive = "删除";
                negative = "取消";
                toastMsg = "对话已删除";
                break;
        }
        
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, (dialog, which) -> {
                    historyManager.deleteHistory(history.getId());
                    if (isSearchMode) {
                        performSearch(searchEditText.getText().toString());
                    } else {
                        loadHistoryData();
                    }
                    safeShowToast(toastMsg);
                })
                .setNegativeButton(negative, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (!isSearchMode) {
            loadHistoryData();
        }
    }
    
    private void safeShowToast(String message) {
        try {
            if (getContext() != null && getActivity() != null && !getActivity().isFinishing()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {}
    }
    
    private void setupKeyboardDismissal(View view) {
        
        if (view instanceof ViewGroup) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        
                        hideKeyboard();
                    }
                    return false;
                }
            });
        }
        
        if (historyRecyclerView != null) {
            historyRecyclerView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        hideKeyboard();
                    }
                    return false;
                }
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
