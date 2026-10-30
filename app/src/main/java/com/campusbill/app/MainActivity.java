package com.campusbill.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    /** 占位：本月支出，任务 13 接入 Room 后替换为真实数据 */
    private static final double MOCK_MONTHLY_EXPENSE = 1860.50;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.etBudget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateBudgetPanel();
            }
        });

        binding.panelAi.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AiChatActivity.class)));

        updateBudgetPanel();
    }

    /** 根据预算输入刷新总预算/剩余数值，并按剩余正负切换渐变背景 */
    private void updateBudgetPanel() {
        double budget = parseAmount(binding.etBudget.getText().toString());
        double remaining = budget - MOCK_MONTHLY_EXPENSE;

        binding.tvTotalBudget.setText(getString(R.string.currency_format, formatAmount(budget)));

        if (remaining >= 0) {
            binding.panelBudget.setBackgroundResource(R.drawable.bg_gradient_blue);
            binding.tvRemainLabel.setText(R.string.label_remaining);
            binding.tvRemainLabel.setTextColor(getColor(R.color.text_secondary));
            binding.tvRemainValue.setTextColor(getColor(R.color.color_primary));
            binding.tvRemainValue.setText(getString(R.string.currency_format, formatAmount(remaining)));
        } else {
            binding.panelBudget.setBackgroundResource(R.drawable.bg_gradient_red);
            binding.tvRemainLabel.setText(R.string.label_remaining);
            binding.tvRemainLabel.setTextColor(getColor(R.color.color_warning));
            binding.tvRemainValue.setTextColor(getColor(R.color.color_warning));
            binding.tvRemainValue.setText(
                    getString(R.string.label_over) + " "
                            + getString(R.string.currency_format, formatAmount(-remaining)));
        }
    }

    private double parseAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatAmount(double amount) {
        return String.format(Locale.CHINA, "%.2f", amount);
    }
}
