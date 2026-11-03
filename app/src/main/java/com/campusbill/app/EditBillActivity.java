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
import com.campusbill.app.databinding.ActivityEditBillBinding;
import com.campusbill.app.ui.PressScaleHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 编辑账单：自动填充原账单信息，支持修改类型、分类、金额、商户、备注与日期时间，
 * 保存写回本机 Room 数据库后返回上一页。全程无网络上传。
 */
public class EditBillActivity extends AppCompatActivity {

    public static final String EXTRA_BILL_ID = "extra_bill_id";

    private ActivityEditBillBinding binding;
    private BillEntity bill;
    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categories = new ArrayList<>();
    private int billType = BillEntity.TYPE_EXPENSE;
    private LocalDate dateValue = LocalDate.now();
    private int hour;
    private int minute;
    private int second;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PressScaleHelper.attach(binding.btnSave);
        binding.btnBack.setOnClickListener(v -> finish());

        setupCategorySpinner();
        binding.tvTypeExpense.setOnClickListener(v -> setBillType(BillEntity.TYPE_EXPENSE));
        binding.tvTypeIncome.setOnClickListener(v -> setBillType(BillEntity.TYPE_INCOME));
        binding.tvDate.setOnClickListener(v -> pickDate());
        binding.tvTime.setOnClickListener(v -> pickTime());
        binding.btnSave.setOnClickListener(v -> save());
        applyBottomInset();

        setBillType(BillEntity.TYPE_EXPENSE);
        updateDateText();
        updateTimeText();

        long billId = getIntent().getLongExtra(EXTRA_BILL_ID, -1);
        loadBill(billId);
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
        String[] items = getResources().getStringArray(R.array.bill_categories);
        categories.addAll(Arrays.asList(items));
        categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerCategory.setAdapter(categoryAdapter);
    }

    /** 从 Room 读取账单并填充表单 */
    private void loadBill(long billId) {
        DBHelper.getInstance(this).getById(billId,
                new DBHelper.DataCallback<BillEntity>() {
                    @Override
                    public void onSuccess(BillEntity data) {
                        if (data == null) {
                            Toast.makeText(EditBillActivity.this,
                                    R.string.edit_load_failed, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        bindBill(data);
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(EditBillActivity.this,
                                R.string.edit_load_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void bindBill(BillEntity data) {
        bill = data;
        setBillType(bill.type);

        // 分类不在预设列表时补充进去，保证原值可保留
        if (bill.category != null && !bill.category.trim().isEmpty()
                && !categories.contains(bill.category)) {
            categories.add(0, bill.category);
            categoryAdapter.notifyDataSetChanged();
        }
        int index = bill.category == null ? -1 : categories.indexOf(bill.category);
        binding.spinnerCategory.setSelection(Math.max(0, index));

        binding.etAmount.setText(formatAmountInput(bill.amount));
        binding.etMerchant.setText(bill.merchant == null ? "" : bill.merchant);
        binding.etRemark.setText(bill.remark == null ? "" : bill.remark);

        dateValue = parseDate(bill.date);
        parseTime(bill.time);
        updateDateText();
        updateTimeText();
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private void parseTime(String time) {
        hour = 0;
        minute = 0;
        second = 0;
        if (time == null) {
            return;
        }
        try {
            String[] parts = time.split(":");
            if (parts.length >= 2) {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }
            if (parts.length >= 3) {
                second = Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException ignored) {
            hour = 0;
            minute = 0;
            second = 0;
        }
    }

    private String formatAmountInput(double amount) {
        if (amount == Math.floor(amount)) {
            return String.format(Locale.CHINA, "%.0f", amount);
        }
        return String.format(Locale.CHINA, "%.2f", amount);
    }

    private void updateDateText() {
        binding.tvDate.setText(dateValue.toString());
    }

    private void updateTimeText() {
        binding.tvTime.setText(String.format(Locale.CHINA, "%02d:%02d:%02d",
                hour, minute, second));
    }

    private void setBillType(int type) {
        billType = type;
        applyToggleStyle(binding.tvTypeExpense, type == BillEntity.TYPE_EXPENSE);
        applyToggleStyle(binding.tvTypeIncome, type == BillEntity.TYPE_INCOME);
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
        if (bill == null) {
            Toast.makeText(this, R.string.edit_load_failed, Toast.LENGTH_SHORT).show();
            return;
        }
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

        bill.type = billType;
        int position = binding.spinnerCategory.getSelectedItemPosition();
        bill.category = position >= 0 ? categories.get(position) : categories.get(0);
        bill.amount = amount;
        bill.merchant = binding.etMerchant.getText().toString().trim();
        bill.remark = binding.etRemark.getText().toString().trim();
        bill.date = dateValue.toString();
        bill.time = String.format(Locale.CHINA, "%02d:%02d:%02d", hour, minute, second);

        DBHelper.getInstance(this).update(bill, new DBHelper.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer rows) {
                if (rows != null && rows > 0) {
                    Toast.makeText(EditBillActivity.this,
                            R.string.edit_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditBillActivity.this,
                            R.string.edit_save_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(EditBillActivity.this,
                        R.string.edit_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
