package com.campusbill.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.campusbill.app.chart.CandyBarChartRenderer;
import com.campusbill.app.chat.ai.BillAnalysisUtils;
import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.databinding.ActivityMainBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /** 示例数据：仅在本机数据库无任何账单时展示，录入账单后自动改为数据库数据 */
    private static final float[] MOCK_DAILY_EXPENSE = {
            40f, 35f, 56f, 92f, 120f, 48f, 210f,
            38f, 45f, 88f, 65f, 260f, 110f, 42f,
            58f, 72f, 135f, 50f, 68f, 85f, 190f,
            46f, 40f, 96f, 150f, 52f, 74f, 620f,
            88f, 60f
    };
    private static final String[] MOCK_CATEGORY_NAME = {
            "餐饮", "购物", "交通", "娱乐", "学习", "通讯", "医疗", "日用", "其他"
    };
    private static final float[] MOCK_CATEGORY_AMOUNT = {
            780f, 420f, 210f, 180f, 150f, 90f, 60f, 120f, 40f
    };
    private static final double MOCK_LAST_MONTH_EXPENSE = 2145.00;
    private static final double MOCK_INCOME = 1800.00;
    private static final String MOCK_TOP_MERCHANT = "学校食堂";
    private static final double MOCK_TOP_AMOUNT = 226.00;

    private ActivityMainBinding binding;
    private DBHelper dbHelper;
    /** 当前面板展示的月份 */
    private YearMonth selectedMonth;
    /** 当前月份的支出合计（真实数据或示例数据） */
    private double monthExpense;
    /** 当前月份的收入合计 */
    private double monthIncome;
    /** 当前月份消费排行第一的商户与金额 */
    private String topMerchant;
    private double topAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DBHelper.getInstance(this);
        selectedMonth = YearMonth.now();

        double savedBudget = BillAnalysisUtils.getBudget(this);
        if (savedBudget > 0) {
            binding.etBudget.setText(formatAmount(savedBudget));
        }

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
                BillAnalysisUtils.saveBudget(MainActivity.this,
                        parseAmount(s == null ? "" : s.toString()));
            }
        });

        binding.panelAi.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AiChatActivity.class)));
        binding.btnViewDetail.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IncomeDetailActivity.class);
            intent.putExtra(IncomeDetailActivity.EXTRA_MONTH, selectedMonth.toString());
            intent.putExtra(IncomeDetailActivity.EXTRA_TYPE, BillEntity.TYPE_EXPENSE);
            startActivity(intent);
        });
        binding.btnMonthPicker.setOnClickListener(v -> showMonthPicker());
        binding.panelIncome.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IncomeDetailActivity.class);
            intent.putExtra(IncomeDetailActivity.EXTRA_MONTH, selectedMonth.toString());
            startActivity(intent);
        });
        binding.panelRank.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MerchantRankActivity.class);
            intent.putExtra(MerchantRankActivity.EXTRA_MONTH, selectedMonth.toString());
            startActivity(intent);
        });
        binding.panelCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            intent.putExtra(CalendarActivity.EXTRA_MONTH, selectedMonth.toString());
            startActivity(intent);
        });
        binding.btnAddBill.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddBillActivity.class)));

        binding.tvChartMonth.setText(selectedMonth.toString());
        updateBudgetPanel();
        loadMonthData();
    }

    /** 点击右上角：弹窗列出本机账单中出现过的月份，由用户选择要查看的月份 */
    private void showMonthPicker() {
        dbHelper.getBillMonths(new DBHelper.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> months) {
                if (months == null || months.isEmpty()) {
                    Toast.makeText(MainActivity.this, R.string.month_picker_empty,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] labels = new String[months.size()];
                int checked = -1;
                for (int i = 0; i < months.size(); i++) {
                    YearMonth month = YearMonth.parse(months.get(i));
                    labels[i] = getString(R.string.month_picker_label_format,
                            month.getYear(), month.getMonthValue());
                    if (months.get(i).equals(selectedMonth.toString())) {
                        checked = i;
                    }
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.month_picker_title)
                        .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                            dialog.dismiss();
                            selectedMonth = YearMonth.parse(months.get(which));
                            loadMonthData();
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(MainActivity.this, R.string.month_picker_empty,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 读取当前月份账单数据：本机已有账单则用真实数据，否则展示示例数据 */
    private void loadMonthData() {
        dbHelper.countAll(new DBHelper.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (count != null && count > 0) {
                    loadRealMonth();
                } else {
                    loadMockMonth();
                }
            }

            @Override
            public void onError(Exception e) {
                loadMockMonth();
            }
        });
    }

    private void loadMockMonth() {
        int days = selectedMonth.lengthOfMonth();
        float[] daily = new float[days];
        double expense = 0;
        for (int i = 0; i < days && i < MOCK_DAILY_EXPENSE.length; i++) {
            daily[i] = MOCK_DAILY_EXPENSE[i];
            expense += daily[i];
        }
        monthIncome = MOCK_INCOME;
        topMerchant = MOCK_TOP_MERCHANT;
        topAmount = MOCK_TOP_AMOUNT;
        renderMonth(expense, daily, MOCK_CATEGORY_NAME, MOCK_CATEGORY_AMOUNT,
                MOCK_LAST_MONTH_EXPENSE);
    }

    private void loadRealMonth() {
        String month = selectedMonth.toString();
        dbHelper.getBillsByMonth(month, new DBHelper.DataCallback<List<BillEntity>>() {
            @Override
            public void onSuccess(List<BillEntity> bills) {
                int days = selectedMonth.lengthOfMonth();
                float[] daily = new float[days];
                Map<String, Double> categoryMap = new HashMap<>();
                Map<String, Double> merchantMap = new HashMap<>();
                double expense = 0;
                double income = 0;

                if (bills != null) {
                    for (BillEntity bill : bills) {
                        if (bill.type == BillEntity.TYPE_INCOME) {
                            income += bill.amount;
                            continue;
                        }
                        if (bill.type != BillEntity.TYPE_EXPENSE) {
                            continue;
                        }
                        int day = parseDayOfMonth(bill.date);
                        if (day >= 1 && day <= days) {
                            daily[day - 1] += (float) bill.amount;
                        }
                        String category = bill.category == null || bill.category.isEmpty()
                                ? "其他" : bill.category;
                        categoryMap.merge(category, bill.amount, Double::sum);
                        String merchant = bill.merchant == null || bill.merchant.isEmpty()
                                ? "未知商户" : bill.merchant;
                        merchantMap.merge(merchant, bill.amount, Double::sum);
                        expense += bill.amount;
                    }
                }

                monthIncome = income;
                topMerchant = "";
                topAmount = 0;
                for (Map.Entry<String, Double> entry : merchantMap.entrySet()) {
                    if (entry.getValue() > topAmount) {
                        topMerchant = entry.getKey();
                        topAmount = entry.getValue();
                    }
                }

                List<Map.Entry<String, Double>> sorted = new ArrayList<>(categoryMap.entrySet());
                sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                String[] names = new String[sorted.size()];
                float[] amounts = new float[sorted.size()];
                for (int i = 0; i < sorted.size(); i++) {
                    names[i] = sorted.get(i).getKey();
                    amounts[i] = sorted.get(i).getValue().floatValue();
                }

                loadPrevMonthExpense(expense, daily, names, amounts);
            }

            @Override
            public void onError(Exception e) {
                loadMockMonth();
            }
        });
    }

    private void loadPrevMonthExpense(double expense, float[] daily,
                                      String[] names, float[] amounts) {
        String previous = selectedMonth.minusMonths(1).toString();
        dbHelper.sumAmountByMonthAndType(previous, BillEntity.TYPE_EXPENSE,
                new DBHelper.DataCallback<Double>() {
                    @Override
                    public void onSuccess(Double prevTotal) {
                        renderMonth(expense, daily, names, amounts,
                                prevTotal == null ? 0 : prevTotal);
                    }

                    @Override
                    public void onError(Exception e) {
                        renderMonth(expense, daily, names, amounts, 0);
                    }
                });
    }

    /** 用指定月份数据刷新面板、图表与预算剩余 */
    private void renderMonth(double expense, float[] daily,
                             String[] categoryNames, float[] categoryAmounts,
                             double prevExpense) {
        monthExpense = expense;
        binding.tvChartMonth.setText(selectedMonth.toString());

        double diff = expense - prevExpense;
        String compareText;
        int colorRes;
        if (diff > 0) {
            compareText = getString(R.string.chart_compare_up, formatAmount(diff));
            colorRes = R.color.color_warning;
        } else if (diff < 0) {
            compareText = getString(R.string.chart_compare_down, formatAmount(-diff));
            colorRes = R.color.color_income;
        } else {
            compareText = getString(R.string.chart_compare_flat, formatAmount(0));
            colorRes = R.color.text_secondary;
        }
        binding.tvMonthCompare.setText(
                getString(R.string.chart_compare_prefix) + "  " + compareText);
        binding.tvMonthCompare.setTextColor(getColor(colorRes));

        setupDailyChart(daily);
        setupCategoryChart(categoryNames, categoryAmounts);
        updateBudgetPanel();
        updateEntriesPanel();
    }

    /** 刷新三张功能入口卡片：本月收入、消费排行 Top1、当月总消费 */
    private void updateEntriesPanel() {
        binding.tvIncomePreview.setText(
                getString(R.string.entry_income_preview, formatAmount(monthIncome)));

        if (topMerchant == null || topMerchant.isEmpty() || topAmount <= 0) {
            binding.tvRankPreview.setText(R.string.entry_rank_empty);
        } else {
            binding.tvRankPreview.setText(getString(R.string.entry_rank_preview,
                    topMerchant, formatAmount(topAmount)));
        }

        binding.tvCalendarAmount.setText(
                getString(R.string.currency_format, formatAmount(monthExpense)));
    }

    /** 每日消费折线图：X 轴展示当月每一天，Y 轴随当月最高单日消费自适应，可横向滑动 */
    private void setupDailyChart(float[] daily) {
        LineChart chart = binding.chartDaily;
        chart.setNoDataText(getString(R.string.chart_no_data));
        chart.setNoDataTextColor(getColor(R.color.candy_ink));

        if (daily == null || daily.length == 0) {
            configureChart(chart, 100f);
            chart.clear();
            chart.invalidate();
            return;
        }

        float maxValue = 0f;
        for (float value : daily) {
            maxValue = Math.max(maxValue, value);
        }
        configureChart(chart, niceAxisMax(maxValue));

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String monthPrefix = selectedMonth.toString().substring(5);
        for (int i = 0; i < daily.length; i++) {
            entries.add(new Entry(i, daily[i]));
            labels.add(String.format(Locale.CHINA, "%s-%02d", monthPrefix, i + 1));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        dataSet.setColor(getColor(R.color.candy_pink));
        dataSet.setLineWidth(2.6f);
        dataSet.setCircleColor(getColor(R.color.candy_pink));
        dataSet.setCircleRadius(3.5f);
        dataSet.setCircleHoleRadius(1.6f);
        dataSet.setCircleHoleColor(getColor(R.color.candy_white));
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{getColor(R.color.candy_pink_soft), 0x00FFFFFF}));

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.setData(new LineData(dataSet));
        chart.setVisibleXRangeMaximum(7f);
        chart.moveViewToX(0f);
        chart.invalidate();
    }

    /** 分类金额柱状图：圆角柱体 + 底部白色向上的糖果色渐变，Y 轴随最大分类金额自适应 */
    private void setupCategoryChart(String[] names, float[] amounts) {
        BarChart chart = binding.chartCategory;
        chart.setRenderer(new CandyBarChartRenderer(
                chart, chart.getAnimator(), chart.getViewPortHandler()));
        chart.setNoDataText(getString(R.string.chart_no_data));
        chart.setNoDataTextColor(getColor(R.color.candy_ink));

        if (names == null || names.length == 0) {
            configureChart(chart, 100f);
            chart.clear();
            chart.invalidate();
            return;
        }

        float maxValue = 0f;
        for (float value : amounts) {
            maxValue = Math.max(maxValue, value);
        }
        configureChart(chart, niceAxisMax(maxValue));

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            entries.add(new BarEntry(i, amounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(new int[]{
                getColor(R.color.candy_pink), getColor(R.color.candy_mint),
                getColor(R.color.candy_sky), getColor(R.color.candy_lemon),
                getColor(R.color.candy_lilac), getColor(R.color.candy_peach),
                getColor(R.color.candy_aqua), getColor(R.color.candy_coral),
                getColor(R.color.candy_gum)
        });
        dataSet.setDrawValues(false);
        dataSet.setHighLightAlpha(0);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(names));
        chart.setData(data);
        chart.setVisibleXRangeMaximum(5f);
        chart.moveViewToX(0f);
        chart.invalidate();
    }

    /** 按当月数据最大值计算 Y 轴上限：留 10% 余量后取整，保证 6 个刻度标签都是整数 */
    private float niceAxisMax(float maxValue) {
        if (maxValue <= 0f) {
            return 100f;
        }
        float[] steps = {5f, 10f, 20f, 25f, 50f, 100f, 200f, 250f, 500f, 1000f, 2000f, 2500f, 5000f};
        float target = maxValue * 1.1f;
        for (float step : steps) {
            float axisMax = step * 5f;
            if (target <= axisMax) {
                return axisMax;
            }
        }
        return (float) (Math.ceil(target / 25000f) * 25000f);
    }

    /** 两张图表共用的坐标轴与交互配置，axisMax 为 Y 轴上限 */
    private void configureChart(BarLineChartBase<?> chart, float axisMax) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraOffsets(4f, 8f, 12f, 8f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(axisMax);
        leftAxis.setLabelCount(6, true);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(getColor(R.color.candy_grid));
        leftAxis.setGridLineWidth(0.6f);
        leftAxis.setTextColor(getColor(R.color.text_secondary));
        leftAxis.setTextSize(9f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return String.valueOf((int) value);
            }
        });
        chart.getAxisRight().setEnabled(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(getColor(R.color.candy_grid));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(getColor(R.color.text_secondary));
        xAxis.setTextSize(9f);
        xAxis.setYOffset(4f);
    }

    /** 根据预算输入刷新总预算/剩余数值，并按剩余正负切换渐变背景 */
    private void updateBudgetPanel() {
        double budget = parseAmount(binding.etBudget.getText().toString());
        double remaining = budget - monthExpense;

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

    /** 从 yyyy-MM-dd 中取出「日」 */
    private int parseDayOfMonth(String date) {
        if (date == null || date.length() < 10) {
            return 0;
        }
        try {
            return Integer.parseInt(date.substring(8, 10));
        } catch (NumberFormatException e) {
            return 0;
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
