package com.campusbill.app;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.campusbill.app.chart.IncomeTimelineView;
import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.databinding.ActivityIncomeDetailBinding;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 收入分类详情：本月收入饼图 + 图例 + 纵向时间轴（从下往上），点击节点查看账单详情。
 * 数据来自本机 Room 数据库（所选月份的收入账单）。
 */
public class IncomeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH = "extra_month";
    public static final String EXTRA_TYPE = "extra_type";

    private static final int[] PALETTE = {
            R.color.candy_mint, R.color.candy_sky, R.color.candy_lemon,
            R.color.candy_lilac, R.color.candy_peach, R.color.candy_pink,
            R.color.candy_aqua, R.color.candy_coral
    };

    private ActivityIncomeDetailBinding binding;
    private final Map<String, Integer> categoryColors = new LinkedHashMap<>();
    private final List<String> pieCategories = new ArrayList<>();
    private final List<Double> pieAmounts = new ArrayList<>();
    private YearMonth month;
    private int billType = BillEntity.TYPE_INCOME;
    private double pieTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncomeDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());

        billType = getIntent().getIntExtra(EXTRA_TYPE, BillEntity.TYPE_INCOME);
        String monthExtra = getIntent().getStringExtra(EXTRA_MONTH);
        month = monthExtra == null ? YearMonth.now() : parseMonth(monthExtra);
        binding.tvMonthLabel.setText(getString(R.string.month_picker_label_format,
                month.getYear(), month.getMonthValue()));
        applyModeUi();

        loadIncomes();
    }

    /** 按收入 / 消费模式切换标题与文案 */
    private void applyModeUi() {
        boolean income = billType == BillEntity.TYPE_INCOME;
        binding.tvTitle.setText(income ? R.string.income_detail_title
                : R.string.expense_detail_title);
        binding.tvTotalLabel.setText(income ? R.string.income_month_total
                : R.string.expense_month_total);
        binding.tvTimelineTitle.setText(income ? R.string.income_timeline_title
                : R.string.expense_timeline_title);
        binding.tvEmptyPie.setText(income ? R.string.income_empty
                : R.string.expense_empty);
        binding.timelineIncome.setEmptyText(getString(income
                ? R.string.income_empty : R.string.expense_empty));
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    /** 从 Room 读取所选月份的收入账单 */
    private void loadIncomes() {
        DBHelper.getInstance(this).getBillsByMonth(month.toString(),
                new DBHelper.DataCallback<List<BillEntity>>() {
                    @Override
                    public void onSuccess(List<BillEntity> data) {
                        List<BillEntity> incomes = new ArrayList<>();
                        if (data != null) {
                            for (BillEntity bill : data) {
                                boolean income = bill.type == BillEntity.TYPE_INCOME;
                                if (billType == BillEntity.TYPE_INCOME ? income : !income) {
                                    incomes.add(bill);
                                }
                            }
                        }
                        render(incomes);
                    }

                    @Override
                    public void onError(Exception e) {
                        render(Collections.emptyList());
                    }
                });
    }

    private void render(List<BillEntity> incomes) {
        categoryColors.clear();
        setupPieChart(incomes);
        setupTimeline(incomes);
    }

    /** 本月收入饼图：分类占比 + 切片颜色 + 下方图例 */
    private void setupPieChart(List<BillEntity> incomes) {
        PieChart chart = binding.chartIncomePie;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setRotationEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setTransparentCircleAlpha(0);
        chart.setHoleRadius(52f);
        chart.setTransparentCircleRadius(56f);
        chart.setCenterTextSize(14f);
        chart.setCenterTextColor(ContextCompat.getColor(this, R.color.candy_ink));

        pieCategories.clear();
        pieAmounts.clear();
        pieTotal = 0;

        if (incomes.isEmpty()) {
            chart.setOnChartValueSelectedListener(null);
            chart.setVisibility(View.GONE);
            binding.tvEmptyPie.setVisibility(View.VISIBLE);
            binding.legendContainer.removeAllViews();
            binding.tvTotalIncome.setText("¥0.00");
            return;
        }

        chart.setVisibility(View.VISIBLE);
        binding.tvEmptyPie.setVisibility(View.GONE);

        Map<String, Double> sums = new LinkedHashMap<>();
        double total = 0;
        for (BillEntity bill : incomes) {
            Double old = sums.get(bill.category);
            sums.put(bill.category, old == null ? bill.amount : old + bill.amount);
            total += bill.amount;
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(sums.entrySet());
        Collections.sort(sorted, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            String category = sorted.get(i).getKey();
            double value = sorted.get(i).getValue();
            int color = ContextCompat.getColor(this, PALETTE[i % PALETTE.length]);
            categoryColors.put(category, color);
            pieCategories.add(category);
            pieAmounts.add(value);
            entries.add(new PieEntry((float) value));
            colors.add(color);
        }
        pieTotal = total;

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        // 点击后选中切片向外弹出（凸起），再次点击空白处复原
        dataSet.setSelectionShift(Utils.convertDpToPixel(12f));
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.candy_ink));
        // 占比过小的切片不显示数字，避免文字互相重叠（图例中仍有完整占比）
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value < 6f ? "" : String.format(Locale.CHINA, "%.1f%%", value);
            }
        });

        chart.setData(new PieData(dataSet));
        chart.setUsePercentValues(true);
        chart.setCenterText("¥" + formatAmount(total));
        chart.invalidate();
        chart.animateY(600);

        // 新拟态交互：选中切片凸起并显示该分类明细，取消选中恢复合计
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // 饼图条目未设置 x 值，按金额匹配回分类索引
                int index = -1;
                for (int i = 0; i < pieAmounts.size(); i++) {
                    if (Math.abs(pieAmounts.get(i) - e.getY()) < 0.01) {
                        index = i;
                        break;
                    }
                }
                if (index < 0) {
                    return;
                }
                String category = pieCategories.get(index);
                double amount = pieAmounts.get(index);
                double ratio = pieTotal > 0 ? amount / pieTotal * 100 : 0;
                chart.setCenterText(category + "\n¥" + formatAmount(amount) + "  "
                        + String.format(Locale.CHINA, "%.0f%%", ratio));
                chart.invalidate();
            }

            @Override
            public void onNothingSelected() {
                chart.setCenterText("¥" + formatAmount(pieTotal));
                chart.invalidate();
            }
        });

        binding.tvTotalIncome.setText("¥" + formatAmount(total));
        buildLegend(sorted, total);
    }

    /** 图例：颜色徽标 + 分类 + 金额 + 占比 */
    private void buildLegend(List<Map.Entry<String, Double>> sorted, double total) {
        binding.legendContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, Double> item : sorted) {
            View row = inflater.inflate(R.layout.item_income_legend,
                    binding.legendContainer, false);
            View dot = row.findViewById(R.id.dotColor);
            TextView name = row.findViewById(R.id.tvLegendName);
            TextView amount = row.findViewById(R.id.tvLegendAmount);
            TextView percent = row.findViewById(R.id.tvLegendPercent);

            Integer color = categoryColors.get(item.getKey());
            if (color != null) {
                dot.getBackground().mutate().setTint(color);
            }
            name.setText(item.getKey());
            amount.setText("¥" + formatAmount(item.getValue()));
            double ratio = total > 0 ? item.getValue() / total * 100 : 0;
            percent.setText(String.format(Locale.CHINA, "%.0f%%", ratio));
            binding.legendContainer.addView(row);
        }
    }

    /** 时间轴：按时间升序传入，最下方为最早一笔，页面可下滑查看全部记录 */
    private void setupTimeline(List<BillEntity> incomes) {
        List<BillEntity> ascending = new ArrayList<>(incomes);
        Collections.sort(ascending, (a, b) -> {
            int byDate = a.date.compareTo(b.date);
            return byDate != 0 ? byDate : a.time.compareTo(b.time);
        });
        binding.timelineIncome.setData(ascending, categoryColors);
        binding.timelineIncome.setOnNodeClickListener((index, bill, color) -> {
            binding.timelineIncome.setSelectedIndex(index);
            showDetailDialog(bill, color);
        });
    }

    /** 点击时间轴节点：圆角淡蓝渐变弹窗展示该笔真实账单信息 */
    private void showDetailDialog(BillEntity bill, int color) {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_income_detail, null);
        TextView amount = content.findViewById(R.id.tvDetailAmount);
        TextView category = content.findViewById(R.id.tvDetailCategory);
        TextView merchant = content.findViewById(R.id.tvDetailMerchant);
        TextView remark = content.findViewById(R.id.tvDetailRemark);
        TextView time = content.findViewById(R.id.tvDetailTime);
        View badge = content.findViewById(R.id.detailColorBadge);

        amount.setText("¥" + formatAmount(bill.amount));
        category.setText(bill.category);
        badge.getBackground().mutate().setTint(color);
        merchant.setText(bill.merchant == null || bill.merchant.isEmpty() ? "无" : bill.merchant);
        remark.setText(bill.remark == null || bill.remark.isEmpty() ? "无" : bill.remark);
        time.setText(bill.date + " " + bill.time);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(content);
        // 弹窗关闭后保留选中行的凸起状态，再点其他节点时切换
        content.findViewById(R.id.btnDialogClose).setOnClickListener(v -> dialog.dismiss());

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    private String formatAmount(double amount) {
        return String.format(Locale.CHINA, "%.2f", amount);
    }
}
