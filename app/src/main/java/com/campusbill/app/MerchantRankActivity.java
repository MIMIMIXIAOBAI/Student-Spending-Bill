package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityMerchantRankBinding;

/** 商户消费排行页占位实现，任务 8 完整开发 */
public class MerchantRankActivity extends AppCompatActivity {

    private ActivityMerchantRankBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMerchantRankBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
