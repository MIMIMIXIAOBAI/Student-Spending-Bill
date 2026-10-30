package com.campusbill.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityCalendarBinding;

/** 消费日历页占位实现，任务 9 完整开发 */
public class CalendarActivity extends AppCompatActivity {

    private ActivityCalendarBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
