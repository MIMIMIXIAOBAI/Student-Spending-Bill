package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityEditBillBinding;

/** 编辑账单页占位实现，任务 10 完整开发；由消费日历的「编辑」按钮进入 */
public class EditBillActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_ID = "extra_bill_id";

    private ActivityEditBillBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());
    }
}
