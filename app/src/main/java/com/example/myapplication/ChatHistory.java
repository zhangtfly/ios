package com.example.myapplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatHistory {
    private String id;
    private String title;
    private String question;
    private String answer;
    private long timestamp;
    private boolean hasThinking;
    private String thinkingContent;
    
    private List<ConversationMessage> conversationMessages;
    
    public static class ConversationMessage {
        public String role; 
        public String content;
        public String thinkingContent; 
        
        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public ConversationMessage(String role, String content, String thinkingContent) {
            this.role = role;
            this.content = content;
            this.thinkingContent = thinkingContent;
        }
    }

    public ChatHistory() {
        this.timestamp = System.currentTimeMillis();
        this.id = String.valueOf(timestamp);
        this.conversationMessages = new ArrayList<>();
    }

    public ChatHistory(String question, String answer) {
        this();
        this.question = question;
        this.answer = answer;
        
        this.title = generateTitle(question);
    }

    public ChatHistory(String question, String answer, String thinkingContent) {
        this(question, answer);
        this.thinkingContent = thinkingContent;
        this.hasThinking = thinkingContent != null && !thinkingContent.isEmpty();
    }

    private String generateTitle(String question) {
        if (question == null || question.isEmpty()) {
            return "新对话";
        }
        if (question.length() <= 20) {
            return question;
        }
        return question.substring(0, 20) + "...";
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
        if (this.title == null || this.title.isEmpty()) {
            this.title = generateTitle(question);
        }
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean hasThinking() {
        return hasThinking;
    }

    public void setHasThinking(boolean hasThinking) {
        this.hasThinking = hasThinking;
    }

    public String getThinkingContent() {
        return thinkingContent;
    }

    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
        this.hasThinking = thinkingContent != null && !thinkingContent.isEmpty();
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    public List<ConversationMessage> getConversationMessages() {
        return conversationMessages;
    }
    
    public void setConversationMessages(List<ConversationMessage> conversationMessages) {
        this.conversationMessages = conversationMessages;
    }
    
    public void addMessage(String role, String content) {
        conversationMessages.add(new ConversationMessage(role, content));
        updateDisplayContent();
    }
    
    public void addMessage(String role, String content, String thinkingContent) {
        conversationMessages.add(new ConversationMessage(role, content, thinkingContent));
        updateDisplayContent();
    }
    
    private void updateDisplayContent() {
        if (conversationMessages.isEmpty()) return;
        
        for (ConversationMessage msg : conversationMessages) {
            if ("user".equals(msg.role)) {
                this.question = msg.content;
                if (this.title == null || this.title.isEmpty()) {
                    this.title = generateTitle(msg.content);
                }
                break;
            }
        }
        
        for (int i = conversationMessages.size() - 1; i >= 0; i--) {
            ConversationMessage msg = conversationMessages.get(i);
            if ("assistant".equals(msg.role)) {
                this.answer = msg.content;
                if (msg.thinkingContent != null && !msg.thinkingContent.isEmpty()) {
                    this.thinkingContent = msg.thinkingContent;
                    this.hasThinking = true;
                }
                break;
            }
        }
    }
}
