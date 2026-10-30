package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityAiChatBinding;

/**
 * AI 对话界面占位页，完整交互在后续任务实现。
 */
public class AiChatActivity extends AppCompatActivity {

    private ActivityAiChatBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAiChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
