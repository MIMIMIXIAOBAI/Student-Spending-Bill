package com.campusbill.app;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.databinding.ActivityCalendarBinding;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 消费日历：按月展示日历，有收支的日期打点标记；点击日期查看当日收入 / 支出汇总与账单列表，
 * 每条账单可编辑或删除。数据来自本机 Room 数据库。
 */
public class CalendarActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH = "extra_month";

    private static final String[] WEEK_LABELS = {"日", "一", "二", "三", "四", "五", "六"};

    private ActivityCalendarBinding binding;
    private DayBillAdapter adapter;
    private YearMonth month;
    private LocalDate selectedDate;
    private final List<BillEntity> monthBills = new ArrayList<>();
    /** 每日收支标记：key 为日号，value[0] 收入、value[1] 支出 */
    private final Map<Integer, double[]> dayMarks = new HashMap<>();
    private boolean resumedOnce;

    @Override
    protected void onResume() {
        super.onResume();
        // 从编辑账单页返回时重新读取，保证列表与日历标记同步
        if (resumedOnce) {
            loadMonthBills();
        }
        resumedOnce = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCalendarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());

        String monthExtra = getIntent().getStringExtra(EXTRA_MONTH);
        month = monthExtra == null ? YearMonth.now() : parseMonth(monthExtra);
        LocalDate today = LocalDate.now();
        selectedDate = month.equals(YearMonth.from(today)) ? today : null;

        adapter = new DayBillAdapter(new DayBillAdapter.Listener() {
            @Override
            public void onEdit(BillEntity bill) {
                openEdit(bill);
            }

            @Override
            public void onDelete(BillEntity bill) {
                showDeleteConfirm(bill);
            }
        });
        binding.rvDayBills.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDayBills.setAdapter(adapter);

        binding.btnPrevMonth.setOnClickListener(v -> switchMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> switchMonth(1));

        buildWeekHeader();
        updateMonthLabel();
        loadMonthBills();
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private void switchMonth(int delta) {
        month = month.plusMonths(delta);
        selectedDate = null;
        updateMonthLabel();
        loadMonthBills();
    }

    private void updateMonthLabel() {
        binding.tvMonthLabel.setText(getString(R.string.month_picker_label_format,
                month.getYear(), month.getMonthValue()));
    }

    /** 星期表头 */
    private void buildWeekHeader() {
        binding.weekHeaderRow.removeAllViews();
        for (String label : WEEK_LABELS) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            tv.setLayoutParams(lp);
            tv.setGravity(Gravity.CENTER);
            tv.setText(label);
            tv.setTextSize(12f);
            tv.setTextColor(ContextCompat.getColor(this, R.color.text_hint));
            binding.weekHeaderRow.addView(tv);
        }
    }

    /** 读取当月全部账单，用于日历标记与当日列表 */
    private void loadMonthBills() {
        DBHelper.getInstance(this).getBillsByMonth(month.toString(),
                new DBHelper.DataCallback<List<BillEntity>>() {
                    @Override
                    public void onSuccess(List<BillEntity> data) {
                        monthBills.clear();
                        if (data != null) {
                            monthBills.addAll(data);
                        }
                        buildDayMarks();
                        renderCalendar();
                        renderDay();
                    }

                    @Override
                    public void onError(Exception e) {
                        monthBills.clear();
                        dayMarks.clear();
                        renderCalendar();
                        renderDay();
                    }
                });
    }

    private void buildDayMarks() {
        dayMarks.clear();
        for (BillEntity bill : monthBills) {
            int day = dayOfMonth(bill.date);
            if (day <= 0) {
                continue;
            }
            double[] sums = dayMarks.get(day);
            if (sums == null) {
                sums = new double[2];
                dayMarks.put(day, sums);
            }
            if (bill.isIncome()) {
                sums[0] += bill.amount;
            } else {
                sums[1] += bill.amount;
            }
        }
    }

    private int dayOfMonth(String date) {
        if (date == null || date.length() < 10) {
            return -1;
        }
        try {
            return LocalDate.parse(date).getDayOfMonth();
        } catch (Exception e) {
            return -1;
        }
    }

    /** 绘制当月日历：固定 6 行，有收支的日期下方打点，选中日期高亮 */
    private void renderCalendar() {
        binding.calendarGrid.removeAllViews();
        LocalDate first = month.atDay(1);
        // LocalDate.getDayOfWeek：周一=1…周日=7，表头从周日开始
        int offset = first.getDayOfWeek().getValue() % 7;
        int days = month.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int row = 0; row < 6; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            for (int col = 0; col < 7; col++) {
                int index = row * 7 + col;
                int day = index - offset + 1;
                TextView cell = createDayCell();
                if (day >= 1 && day <= days) {
                    bindDayCell(cell, day, today);
                }
                rowLayout.addView(cell);
            }
            binding.calendarGrid.addView(rowLayout);
        }
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private TextView createDayCell() {
        TextView cell = new TextView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                (int) (40f * density()), 1f);
        cell.setLayoutParams(lp);
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(13f);
        cell.setTextColor(ContextCompat.getColor(this, R.color.candy_ink));
        return cell;
    }

    private void bindDayCell(TextView cell, int day, LocalDate today) {
        cell.setText(String.format(Locale.CHINA, "%d", day));
        boolean isSelected = selectedDate != null
                && selectedDate.getDayOfMonth() == day
                && YearMonth.from(selectedDate).equals(month);
        boolean isToday = today.getYear() == month.getYear()
                && today.getMonthValue() == month.getMonthValue()
                && today.getDayOfMonth() == day;

        if (isSelected) {
            cell.setBackgroundResource(R.drawable.bg_calendar_day_selected);
            cell.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (isToday) {
            cell.setTextColor(ContextCompat.getColor(this, R.color.color_primary));
            cell.setTypeface(Typeface.DEFAULT_BOLD);
        }

        double[] sums = dayMarks.get(day);
        if (sums != null) {
            Drawable dot = ContextCompat.getDrawable(this,
                    R.drawable.bg_calendar_dot).mutate();
            boolean hasIncome = sums[0] > 0;
            boolean hasExpense = sums[1] > 0;
            int color = hasIncome && hasExpense ? R.color.candy_lilac
                    : hasIncome ? R.color.candy_mint : R.color.candy_coral;
            dot.setTint(ContextCompat.getColor(this, color));
            cell.setCompoundDrawablesWithIntrinsicBounds(null, null, null, dot);
            cell.setCompoundDrawablePadding((int) (3f * density()));
        }

        cell.setOnClickListener(v -> {
            selectedDate = month.atDay(day);
            renderCalendar();
            renderDay();
        });
    }

    /** 展示所选日期的收支汇总与账单列表 */
    private void renderDay() {
        if (selectedDate == null) {
            binding.tvSummaryDate.setText(R.string.calendar_select_hint);
            binding.tvDayIncome.setText("--");
            binding.tvDayExpense.setText("--");
            adapter.setData(Collections.emptyList());
            binding.tvDayEmpty.setText(R.string.calendar_select_hint);
            binding.tvDayEmpty.setVisibility(View.VISIBLE);
            binding.rvDayBills.setVisibility(View.GONE);
            return;
        }

        binding.tvSummaryDate.setText(getString(R.string.calendar_day_label_format,
                selectedDate.getYear(), selectedDate.getMonthValue(),
                selectedDate.getDayOfMonth()));

        String dateText = selectedDate.toString();
        List<BillEntity> dayBills = new ArrayList<>();
        double income = 0;
        double expense = 0;
        for (BillEntity bill : monthBills) {
            if (dateText.equals(bill.date)) {
                dayBills.add(bill);
                if (bill.isIncome()) {
                    income += bill.amount;
                } else {
                    expense += bill.amount;
                }
            }
        }
        Collections.sort(dayBills, (a, b) -> {
            String ta = a.time == null ? "" : a.time;
            String tb = b.time == null ? "" : b.time;
            return ta.compareTo(tb);
        });

        binding.tvDayIncome.setText(String.format(Locale.CHINA, "¥%.2f", income));
        binding.tvDayExpense.setText(String.format(Locale.CHINA, "¥%.2f", expense));
        adapter.setData(dayBills);
        binding.tvDayEmpty.setText(R.string.calendar_empty_day);
        boolean empty = dayBills.isEmpty();
        binding.tvDayEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvDayBills.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void openEdit(BillEntity bill) {
        android.content.Intent intent = new android.content.Intent(this,
                EditBillActivity.class);
        intent.putExtra(EditBillActivity.EXTRA_BILL_ID, bill.id);
        startActivity(intent);
    }

    private void showDeleteConfirm(BillEntity bill) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.bill_delete_confirm_title)
                .setMessage(R.string.bill_delete_confirm_message)
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.bill_delete, (dialog, which) -> deleteBill(bill))
                .show();
    }

    private void deleteBill(BillEntity bill) {
        DBHelper.getInstance(this).deleteById(bill.id,
                new DBHelper.DataCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer rows) {
                        Toast.makeText(CalendarActivity.this,
                                R.string.bill_deleted, Toast.LENGTH_SHORT).show();
                        loadMonthBills();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(CalendarActivity.this,
                                R.string.bill_delete_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
