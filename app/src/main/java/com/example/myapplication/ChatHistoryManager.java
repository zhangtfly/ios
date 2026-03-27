package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatHistoryManager {
    private static ChatHistoryManager instance;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "chat_history";
    private static final String KEY_HISTORY_LIST = "history_list";
    private static final int MAX_HISTORY_COUNT = 100; 

    private ChatHistoryManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ChatHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatHistoryManager(context.getApplicationContext());
        }
        return instance;
    }
    
    public void addChatHistory(ChatHistory chatHistory) {
        List<ChatHistory> historyList = getAllHistory();
        
        historyList.add(0, chatHistory);
        
        if (historyList.size() > MAX_HISTORY_COUNT) {
            historyList = historyList.subList(0, MAX_HISTORY_COUNT);
        }
        
        saveHistoryList(historyList);
    }
    
    public void updateChatHistory(ChatHistory chatHistory) {
        List<ChatHistory> historyList = getAllHistory();
        
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getId().equals(chatHistory.getId())) {
                historyList.set(i, chatHistory);
                saveHistoryList(historyList);
                return;
            }
        }
        
        addChatHistory(chatHistory);
    }
    
    public List<ChatHistory> getAllHistory() {
        String jsonString = preferences.getString(KEY_HISTORY_LIST, "[]");
        List<ChatHistory> historyList = new ArrayList<>();
        
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ChatHistory history = jsonToHistory(jsonObject);
                if (history != null) {
                    historyList.add(history);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        Collections.sort(historyList, new Comparator<ChatHistory>() {
            @Override
            public int compare(ChatHistory h1, ChatHistory h2) {
                return Long.compare(h2.getTimestamp(), h1.getTimestamp());
            }
        });
        
        return historyList;
    }
    
    public ChatHistory getHistoryById(String id) {
        List<ChatHistory> historyList = getAllHistory();
        for (ChatHistory history : historyList) {
            if (history.getId().equals(id)) {
                return history;
            }
        }
        return null;
    }
    
    public void deleteHistory(String id) {
        List<ChatHistory> historyList = getAllHistory();
        historyList.removeIf(history -> history.getId().equals(id));
        saveHistoryList(historyList);
    }
    
    public void clearAllHistory() {
        preferences.edit().remove(KEY_HISTORY_LIST).apply();
    }
    
    public List<ChatHistory> searchHistory(String keyword) {
        List<ChatHistory> allHistory = getAllHistory();
        List<ChatHistory> searchResults = new ArrayList<>();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return allHistory;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        for (ChatHistory history : allHistory) {
            if ((history.getQuestion() != null && history.getQuestion().toLowerCase().contains(lowerKeyword)) ||
                (history.getAnswer() != null && history.getAnswer().toLowerCase().contains(lowerKeyword)) ||
                (history.getTitle() != null && history.getTitle().toLowerCase().contains(lowerKeyword))) {
                searchResults.add(history);
            }
        }
        
        return searchResults;
    }
    
    private void saveHistoryList(List<ChatHistory> historyList) {
        JSONArray jsonArray = new JSONArray();
        
        for (ChatHistory history : historyList) {
            JSONObject jsonObject = historyToJson(history);
            if (jsonObject != null) {
                jsonArray.put(jsonObject);
            }
        }
        
        preferences.edit().putString(KEY_HISTORY_LIST, jsonArray.toString()).apply();
    }
    
    private JSONObject historyToJson(ChatHistory history) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", history.getId());
            jsonObject.put("title", history.getTitle());
            jsonObject.put("question", history.getQuestion());
            jsonObject.put("answer", history.getAnswer());
            jsonObject.put("timestamp", history.getTimestamp());
            jsonObject.put("hasThinking", history.hasThinking());
            jsonObject.put("thinkingContent", history.getThinkingContent());
            
            JSONArray messagesArray = new JSONArray();
            if (history.getConversationMessages() != null) {
                for (ChatHistory.ConversationMessage msg : history.getConversationMessages()) {
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", msg.role);
                    msgObj.put("content", msg.content);
                    if (msg.thinkingContent != null) {
                        msgObj.put("thinkingContent", msg.thinkingContent);
                    }
                    messagesArray.put(msgObj);
                }
            }
            jsonObject.put("conversationMessages", messagesArray);
            
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private ChatHistory jsonToHistory(JSONObject jsonObject) {
        try {
            ChatHistory history = new ChatHistory();
            history.setId(jsonObject.optString("id"));
            history.setTitle(jsonObject.optString("title"));
            history.setQuestion(jsonObject.optString("question"));
            history.setAnswer(jsonObject.optString("answer"));
            history.setTimestamp(jsonObject.optLong("timestamp"));
            history.setHasThinking(jsonObject.optBoolean("hasThinking"));
            history.setThinkingContent(jsonObject.optString("thinkingContent"));
            
            JSONArray messagesArray = jsonObject.optJSONArray("conversationMessages");
            if (messagesArray != null) {
                List<ChatHistory.ConversationMessage> messages = new ArrayList<>();
                for (int i = 0; i < messagesArray.length(); i++) {
                    JSONObject msgObj = messagesArray.getJSONObject(i);
                    String role = msgObj.optString("role");
                    String content = msgObj.optString("content");
                    String thinkingContent = msgObj.optString("thinkingContent", null);
                    
                    if (thinkingContent != null && !thinkingContent.isEmpty()) {
                        messages.add(new ChatHistory.ConversationMessage(role, content, thinkingContent));
                    } else {
                        messages.add(new ChatHistory.ConversationMessage(role, content));
                    }
                }
                history.setConversationMessages(messages);
            }
            
            return history;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public int getHistoryCount() {
        return getAllHistory().size();
    }
}
