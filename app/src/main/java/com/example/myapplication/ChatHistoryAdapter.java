package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<ChatHistoryAdapter.HistoryViewHolder> {
    
    private List<ChatHistory> historyList;
    private OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryClick(ChatHistory history);
        void onHistoryDelete(ChatHistory history);
    }

    public ChatHistoryAdapter() {
        this.historyList = new ArrayList<>();
    }

    public void setOnHistoryItemClickListener(OnHistoryItemClickListener listener) {
        this.listener = listener;
    }

    public void updateHistoryList(List<ChatHistory> newHistoryList) {
        this.historyList.clear();
        if (newHistoryList != null) {
            this.historyList.addAll(newHistoryList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ChatHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView timeTextView;
        private TextView questionTextView;
        private TextView answerTextView;
        private TextView thinkingIndicator;
        private ImageButton deleteButton;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            questionTextView = itemView.findViewById(R.id.questionTextView);
            answerTextView = itemView.findViewById(R.id.answerTextView);
            thinkingIndicator = itemView.findViewById(R.id.thinkingIndicator);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(ChatHistory history) {
            
            titleTextView.setText(history.getTitle() != null ? history.getTitle() : "新对话");
            
            timeTextView.setText(history.getFormattedTime());
            
            String question = history.getQuestion();
            if (question != null && !question.isEmpty()) {
                questionTextView.setText("问：" + question);
                questionTextView.setVisibility(View.VISIBLE);
            } else {
                questionTextView.setVisibility(View.GONE);
            }
            
            String answer = history.getAnswer();
            if (answer != null && !answer.isEmpty()) {
                
                String plainAnswer = answer.replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
                                          .replaceAll("\\*([^*]+)\\*", "$1")
                                          .replaceAll("```[\\s\\S]*?```", "[代码]")
                                          .replaceAll("`([^`]+)`", "$1")
                                          .replaceAll("#{1,6}\\s*", "")
                                          .replaceAll("\\n+", " ")
                                          .trim();
                answerTextView.setText("答：" + plainAnswer);
                answerTextView.setVisibility(View.VISIBLE);
            } else {
                answerTextView.setVisibility(View.GONE);
            }
            
            if (history.hasThinking()) {
                thinkingIndicator.setVisibility(View.VISIBLE);
            } else {
                thinkingIndicator.setVisibility(View.GONE);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryClick(history);
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryDelete(history);
                }
            });
        }
    }
}
