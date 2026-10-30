package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityAddBillBinding;

/** 手动记账页占位实现，任务 11 完整开发 */
public class AddBillActivity extends AppCompatActivity {

    private ActivityAddBillBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
