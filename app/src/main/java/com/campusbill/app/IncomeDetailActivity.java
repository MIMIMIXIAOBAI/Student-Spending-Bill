package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityIncomeDetailBinding;

/** 收入分类详情页占位实现，任务 7 完整开发 */
public class IncomeDetailActivity extends AppCompatActivity {

    private ActivityIncomeDetailBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncomeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
