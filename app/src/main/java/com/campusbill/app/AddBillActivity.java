package com.campusbill.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.databinding.ActivityAddBillBinding;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 手动记一笔：选择收支类型后录入金额、分类、商户、备注与日期时间，
 * 保存到本机 Room 数据库后返回主界面。全程无网络上传。
 */
public class AddBillActivity extends AppCompatActivity {

    private ActivityAddBillBinding binding;
    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categories = new ArrayList<>();
    private String[] expenseCategories;
    private String[] incomeCategories;
    private int billType = BillEntity.TYPE_EXPENSE;
    private LocalDate dateValue = LocalDate.now();
    private int hour;
    private int minute;
    private int second;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());

        expenseCategories = getResources().getStringArray(R.array.expense_categories);
        incomeCategories = getResources().getStringArray(R.array.income_categories);

        setupCategorySpinner();
        binding.tvTypeExpense.setOnClickListener(v -> setBillType(BillEntity.TYPE_EXPENSE));
        binding.tvTypeIncome.setOnClickListener(v -> setBillType(BillEntity.TYPE_INCOME));
        binding.tvDate.setOnClickListener(v -> pickDate());
        binding.tvTime.setOnClickListener(v -> pickTime());
        binding.btnSave.setOnClickListener(v -> save());
        applyBottomInset();

        LocalTime now = LocalTime.now();
        hour = now.getHour();
        minute = now.getMinute();
        second = now.getSecond();

        setBillType(BillEntity.TYPE_EXPENSE);
        updateDateText();
        updateTimeText();
    }

    /** 避让系统手势导航栏，保证底部隐私提示完整可见 */
    private void applyBottomInset() {
        final int side = (int) (16 * getResources().getDisplayMetrics().density);
        final int top = (int) (10 * getResources().getDisplayMetrics().density);
        final int baseBottom = (int) (16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            binding.tvPrivacyTip.setPadding(side, top, side, baseBottom + bottom);
            return insets;
        });
    }

    private void setupCategorySpinner() {
        categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setBillType(int type) {
        billType = type;
        applyToggleStyle(binding.tvTypeExpense, type == BillEntity.TYPE_EXPENSE);
        applyToggleStyle(binding.tvTypeIncome, type == BillEntity.TYPE_INCOME);
        switchCategories(type == BillEntity.TYPE_INCOME);
    }

    /** 切换收支类型时对应切换分类下拉选项 */
    private void switchCategories(boolean income) {
        categories.clear();
        String[] source = income ? incomeCategories : expenseCategories;
        for (String item : source) {
            categories.add(item);
        }
        categoryAdapter.notifyDataSetChanged();
        binding.spinnerCategory.setSelection(0);
    }

    private void applyToggleStyle(TextView target, boolean selected) {
        if (selected) {
            boolean income = target.getId() == R.id.tvTypeIncome;
            target.setBackgroundResource(income
                    ? R.drawable.bg_toggle_income
                    : R.drawable.bg_toggle_expense);
            target.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            target.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            target.setBackground(null);
            target.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            target.setTypeface(Typeface.DEFAULT);
        }
    }

    private void updateDateText() {
        binding.tvDate.setText(dateValue.toString());
    }

    private void updateTimeText() {
        binding.tvTime.setText(String.format(Locale.CHINA, "%02d:%02d:%02d",
                hour, minute, second));
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            dateValue = LocalDate.of(year, month + 1, day);
            updateDateText();
        }, dateValue.getYear(), dateValue.getMonthValue() - 1,
                dateValue.getDayOfMonth()).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            hour = hourOfDay;
            minute = minuteOfHour;
            updateTimeText();
        }, hour, minute, true).show();
    }

    private void save() {
        String amountText = binding.etAmount.getText().toString().trim();
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.edit_amount_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount <= 0) {
            Toast.makeText(this, R.string.edit_amount_zero, Toast.LENGTH_SHORT).show();
            return;
        }

        BillEntity bill = new BillEntity();
        bill.type = billType;
        int position = binding.spinnerCategory.getSelectedItemPosition();
        bill.category = position >= 0 ? categories.get(position) : categories.get(0);
        bill.amount = amount;
        bill.merchant = binding.etMerchant.getText().toString().trim();
        bill.remark = binding.etRemark.getText().toString().trim();
        bill.date = dateValue.toString();
        bill.time = String.format(Locale.CHINA, "%02d:%02d:%02d", hour, minute, second);
        bill.createTime = System.currentTimeMillis();

        DBHelper.getInstance(this).insert(bill, new DBHelper.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                if (id != null && id > 0) {
                    Toast.makeText(AddBillActivity.this,
                            R.string.add_bill_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddBillActivity.this,
                            R.string.edit_save_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(AddBillActivity.this,
                        R.string.edit_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
