package com.campusbill.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campusbill.app.R.color;
import com.campusbill.app.R.drawable;
import com.campusbill.app.R.string;
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
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.LineDataSet.Mode;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
   private ActivityMainBinding binding;
   private DBHelper dbHelper;
   private YearMonth selectedMonth;
   private boolean resumedOnce;
   private boolean emptyState;
   private double monthExpense;
   private double monthIncome;
   private String topMerchant;
   private double topAmount;

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
      this.setContentView(this.binding.getRoot());
      this.dbHelper = DBHelper.getInstance(this);
      this.selectedMonth = YearMonth.now();
      double savedBudget = BillAnalysisUtils.getBudget(this);
      if (savedBudget > (double)0.0F) {
         this.binding.etBudget.setText(this.formatAmount(savedBudget));
      }

      this.binding.etBudget.addTextChangedListener(new TextWatcher() {
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {
         }

         public void onTextChanged(CharSequence s, int start, int before, int count) {
         }

         public void afterTextChanged(Editable s) {
            MainActivity.this.updateBudgetPanel();
            BillAnalysisUtils.saveBudget(MainActivity.this, MainActivity.this.parseAmount(s == null ? "" : s.toString()));
         }
      });
      this.binding.panelAi.setOnClickListener((v) -> this.startActivity(new Intent(this, AiChatActivity.class)));
      this.binding.btnViewDetail.setOnClickListener((v) -> {
         Intent intent = new Intent(this, IncomeDetailActivity.class);
         intent.putExtra("extra_month", this.selectedMonth.toString());
         intent.putExtra("extra_type", 0);
         this.startActivity(intent);
      });
      this.binding.btnMonthPicker.setOnClickListener((v) -> this.showMonthPicker());
      this.binding.panelIncome.setOnClickListener((v) -> {
         Intent intent = new Intent(this, IncomeDetailActivity.class);
         intent.putExtra("extra_month", this.selectedMonth.toString());
         this.startActivity(intent);
      });
      this.binding.panelRank.setOnClickListener((v) -> {
         Intent intent = new Intent(this, MerchantRankActivity.class);
         intent.putExtra("extra_month", this.selectedMonth.toString());
         this.startActivity(intent);
      });
      this.binding.panelCalendar.setOnClickListener((v) -> {
         Intent intent = new Intent(this, CalendarActivity.class);
         intent.putExtra("extra_month", this.selectedMonth.toString());
         this.startActivity(intent);
      });
      this.binding.btnAddBill.setOnClickListener((v) -> this.startActivity(new Intent(this, AddBillActivity.class)));
      this.binding.btnImport.setOnClickListener((v) -> this.startActivity(new Intent(this, ImportActivity.class)));
      this.binding.btnHistory.setOnClickListener((v) -> this.startActivity(new Intent(this, HistoryActivity.class)));
      this.binding.tvChartMonth.setText(this.selectedMonth.toString());
      this.updateBudgetPanel();
      this.loadMonthData();
   }

   protected void onResume() {
      super.onResume();
      if (this.resumedOnce) {
         this.loadMonthData();
      } else {
         this.resumedOnce = true;
      }

   }

   private void showMonthPicker() {
      this.dbHelper.getBillMonths(new DBHelper.DataCallback<List<String>>() {
         public void onSuccess(List<String> months) {
            if (months != null && !months.isEmpty()) {
               String[] labels = new String[months.size()];
               int checked = -1;

               for(int i = 0; i < months.size(); ++i) {
                  YearMonth month = YearMonth.parse((CharSequence)months.get(i));
                  labels[i] = MainActivity.this.getString(string.month_picker_label_format, new Object[]{month.getYear(), month.getMonthValue()});
                  if (((String)months.get(i)).equals(MainActivity.this.selectedMonth.toString())) {
                     checked = i;
                  }
               }

               (new AlertDialog.Builder(MainActivity.this)).setTitle(string.month_picker_title).setSingleChoiceItems(labels, checked, (dialog, which) -> {
                  dialog.dismiss();
                  MainActivity.this.selectedMonth = YearMonth.parse((CharSequence)months.get(which));
                  MainActivity.this.loadMonthData();
               }).setNegativeButton(string.btn_cancel, (DialogInterface.OnClickListener)null).show();
            } else {
               Toast.makeText(MainActivity.this, string.month_picker_empty, 0).show();
            }
         }

         public void onError(Exception e) {
            Toast.makeText(MainActivity.this, string.month_picker_empty, 0).show();
         }
      });
   }

   private void loadMonthData() {
      this.dbHelper.countAll(new DBHelper.DataCallback<Integer>() {
         public void onSuccess(Integer count) {
            if (count != null && count > 0) {
               MainActivity.this.loadRealMonth();
            } else {
               MainActivity.this.loadEmptyState();
            }

         }

         public void onError(Exception e) {
            MainActivity.this.loadEmptyState();
         }
      });
   }

   private void loadEmptyState() {
      this.emptyState = true;
      this.monthExpense = (double)0.0F;
      this.monthIncome = (double)0.0F;
      this.topMerchant = "";
      this.topAmount = (double)0.0F;
      this.binding.tvAiSummary.setText(string.ai_panel_empty);
      this.binding.tvMonthCompare.setText(string.main_empty_hint);
      this.binding.tvMonthCompare.setTextColor(this.getColor(color.text_secondary));
      this.binding.tvIncomePreview.setText(string.main_empty_hint);
      this.binding.tvRankPreview.setText(string.main_empty_hint);
      this.binding.tvCalendarAmount.setText(string.main_entry_empty);
      this.setupDailyChart(new float[0]);
      this.setupCategoryChart(new String[0], new float[0]);
      this.updateBudgetPanel();
   }

   private void loadRealMonth() {
      this.emptyState = false;
      this.binding.tvAiSummary.setText(string.ai_panel_summary);
      String month = this.selectedMonth.toString();
      this.dbHelper.getBillsByMonth(month, new DBHelper.DataCallback<List<BillEntity>>() {
         public void onSuccess(List<BillEntity> bills) {
            int days = MainActivity.this.selectedMonth.lengthOfMonth();
            float[] daily = new float[days];
            Map<String, Double> categoryMap = new HashMap();
            Map<String, Double> merchantMap = new HashMap();
            double expense = (double)0.0F;
            double income = (double)0.0F;
            if (bills != null) {
               for(BillEntity bill : bills) {
                  if (bill.type == 1) {
                     income += bill.amount;
                  } else if (bill.type == 0) {
                     int day = MainActivity.this.parseDayOfMonth(bill.date);
                     if (day >= 1 && day <= days) {
                        daily[day - 1] += (float)bill.amount;
                     }

                     String category = bill.category != null && !bill.category.isEmpty() ? bill.category : "其他";
                     categoryMap.merge(category, bill.amount, Double::sum);
                     String merchant = bill.merchant != null && !bill.merchant.isEmpty() ? bill.merchant : "未知商户";
                     merchantMap.merge(merchant, bill.amount, Double::sum);
                     expense += bill.amount;
                  }
               }
            }

            MainActivity.this.monthIncome = income;
            MainActivity.this.topMerchant = "";
            MainActivity.this.topAmount = (double)0.0F;

            for(Map.Entry<String, Double> entry : merchantMap.entrySet()) {
               if ((Double)entry.getValue() > MainActivity.this.topAmount) {
                  MainActivity.this.topMerchant = (String)entry.getKey();
                  MainActivity.this.topAmount = (Double)entry.getValue();
               }
            }

            List<Map.Entry<String, Double>> sorted = new ArrayList(categoryMap.entrySet());
            sorted.sort((a, b) -> Double.compare((Double)b.getValue(), (Double)a.getValue()));
            String[] names = new String[sorted.size()];
            float[] amounts = new float[sorted.size()];

            for(int i = 0; i < sorted.size(); ++i) {
               names[i] = (String)((Map.Entry)sorted.get(i)).getKey();
               amounts[i] = ((Double)((Map.Entry)sorted.get(i)).getValue()).floatValue();
            }

            MainActivity.this.loadPrevMonthExpense(expense, daily, names, amounts);
         }

         public void onError(Exception e) {
            MainActivity.this.loadEmptyState();
         }
      });
   }

   private void loadPrevMonthExpense(final double expense, final float[] daily, final String[] names, final float[] amounts) {
      String previous = this.selectedMonth.minusMonths(1L).toString();
      this.dbHelper.sumAmountByMonthAndType(previous, 0, new DBHelper.DataCallback<Double>() {
         public void onSuccess(Double prevTotal) {
            MainActivity.this.renderMonth(expense, daily, names, amounts, prevTotal == null ? (double)0.0F : prevTotal);
         }

         public void onError(Exception e) {
            MainActivity.this.renderMonth(expense, daily, names, amounts, (double)0.0F);
         }
      });
   }

   private void renderMonth(double expense, float[] daily, String[] categoryNames, float[] categoryAmounts, double prevExpense) {
      this.monthExpense = expense;
      this.binding.tvChartMonth.setText(this.selectedMonth.toString());
      double diff = expense - prevExpense;
      String compareText;
      int colorRes;
      if (diff > (double)0.0F) {
         compareText = this.getString(string.chart_compare_up, new Object[]{this.formatAmount(diff)});
         colorRes = color.color_warning;
      } else if (diff < (double)0.0F) {
         compareText = this.getString(string.chart_compare_down, new Object[]{this.formatAmount(-diff)});
         colorRes = color.color_income;
      } else {
         compareText = this.getString(string.chart_compare_flat, new Object[]{this.formatAmount((double)0.0F)});
         colorRes = color.text_secondary;
      }

      this.binding.tvMonthCompare.setText(this.getString(string.chart_compare_prefix) + "  " + compareText);
      this.binding.tvMonthCompare.setTextColor(this.getColor(colorRes));
      this.setupDailyChart(daily);
      this.setupCategoryChart(categoryNames, categoryAmounts);
      this.updateBudgetPanel();
      this.updateEntriesPanel();
   }

   private void updateEntriesPanel() {
      this.binding.tvIncomePreview.setText(this.getString(string.entry_income_preview, new Object[]{this.formatAmount(this.monthIncome)}));
      if (this.topMerchant != null && !this.topMerchant.isEmpty() && !(this.topAmount <= (double)0.0F)) {
         this.binding.tvRankPreview.setText(this.getString(string.entry_rank_preview, new Object[]{this.topMerchant, this.formatAmount(this.topAmount)}));
      } else {
         this.binding.tvRankPreview.setText(string.entry_rank_empty);
      }

      this.binding.tvCalendarAmount.setText(this.getString(string.currency_format, new Object[]{this.formatAmount(this.monthExpense)}));
   }

   private void setupDailyChart(float[] daily) {
      LineChart chart = this.binding.chartDaily;
      chart.setNoDataText(this.getString(this.emptyState ? string.main_empty_hint : string.chart_no_data));
      chart.setNoDataTextColor(this.getColor(color.candy_ink));
      if (daily != null && daily.length != 0) {
         float maxValue = 0.0F;

         for(float value : daily) {
            maxValue = Math.max(maxValue, value);
         }

         this.configureChart(chart, this.niceAxisMax(maxValue));
         List<Entry> entries = new ArrayList();
         List<String> labels = new ArrayList();
         String monthPrefix = this.selectedMonth.toString().substring(5);

         for(int i = 0; i < daily.length; ++i) {
            entries.add(new Entry((float)i, daily[i]));
            labels.add(String.format(Locale.CHINA, "%s-%02d", monthPrefix, i + 1));
         }

         LineDataSet dataSet = new LineDataSet(entries, "");
         dataSet.setMode(Mode.HORIZONTAL_BEZIER);
         dataSet.setColor(this.getColor(color.candy_pink));
         dataSet.setLineWidth(2.6F);
         dataSet.setCircleColor(this.getColor(color.candy_pink));
         dataSet.setCircleRadius(3.5F);
         dataSet.setCircleHoleRadius(1.6F);
         dataSet.setCircleHoleColor(this.getColor(color.candy_white));
         dataSet.setDrawValues(false);
         dataSet.setDrawFilled(true);
         dataSet.setFillDrawable(new GradientDrawable(Orientation.TOP_BOTTOM, new int[]{this.getColor(color.candy_pink_soft), 16777215}));
         chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
         chart.setData(new LineData(new ILineDataSet[]{dataSet}));
         chart.setVisibleXRangeMaximum(7.0F);
         chart.moveViewToX(0.0F);
         chart.invalidate();
      } else {
         this.configureChart(chart, 100.0F);
         chart.clear();
         chart.invalidate();
      }
   }

   private void setupCategoryChart(String[] names, float[] amounts) {
      BarChart chart = this.binding.chartCategory;
      chart.setRenderer(new CandyBarChartRenderer(chart, chart.getAnimator(), chart.getViewPortHandler()));
      chart.setNoDataText(this.getString(this.emptyState ? string.main_empty_hint : string.chart_no_data));
      chart.setNoDataTextColor(this.getColor(color.candy_ink));
      if (names != null && names.length != 0) {
         float maxValue = 0.0F;

         for(float value : amounts) {
            maxValue = Math.max(maxValue, value);
         }

         this.configureChart(chart, this.niceAxisMax(maxValue));
         List<BarEntry> entries = new ArrayList();

         for(int i = 0; i < amounts.length; ++i) {
            entries.add(new BarEntry((float)i, amounts[i]));
         }

         BarDataSet dataSet = new BarDataSet(entries, "");
         dataSet.setColors(new int[]{this.getColor(color.candy_pink), this.getColor(color.candy_mint), this.getColor(color.candy_sky), this.getColor(color.candy_lemon), this.getColor(color.candy_lilac), this.getColor(color.candy_peach), this.getColor(color.candy_aqua), this.getColor(color.candy_coral), this.getColor(color.candy_gum)});
         dataSet.setDrawValues(false);
         dataSet.setHighLightAlpha(0);
         BarData data = new BarData(new IBarDataSet[]{dataSet});
         data.setBarWidth(0.6F);
         chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(names));
         chart.setData(data);
         chart.setVisibleXRangeMaximum(5.0F);
         chart.moveViewToX(0.0F);
         chart.invalidate();
      } else {
         this.configureChart(chart, 100.0F);
         chart.clear();
         chart.invalidate();
      }
   }

   private float niceAxisMax(float maxValue) {
      if (maxValue <= 0.0F) {
         return 100.0F;
      } else {
         float[] steps = new float[]{5.0F, 10.0F, 20.0F, 25.0F, 50.0F, 100.0F, 200.0F, 250.0F, 500.0F, 1000.0F, 2000.0F, 2500.0F, 5000.0F};
         float target = maxValue * 1.1F;

         for(float step : steps) {
            float axisMax = step * 5.0F;
            if (target <= axisMax) {
               return axisMax;
            }
         }

         return (float)(Math.ceil((double)(target / 25000.0F)) * (double)25000.0F);
      }
   }

   private void configureChart(BarLineChartBase<?> chart, float axisMax) {
      chart.getDescription().setEnabled(false);
      chart.getLegend().setEnabled(false);
      chart.setDrawGridBackground(false);
      chart.setTouchEnabled(true);
      chart.setDragEnabled(true);
      chart.setScaleEnabled(false);
      chart.setPinchZoom(false);
      chart.setDoubleTapToZoomEnabled(false);
      chart.setExtraOffsets(4.0F, 8.0F, 12.0F, 8.0F);
      YAxis leftAxis = chart.getAxisLeft();
      leftAxis.setAxisMinimum(0.0F);
      leftAxis.setAxisMaximum(axisMax);
      leftAxis.setLabelCount(6, true);
      leftAxis.setDrawAxisLine(false);
      leftAxis.setDrawGridLines(true);
      leftAxis.setGridColor(this.getColor(color.candy_grid));
      leftAxis.setGridLineWidth(0.6F);
      leftAxis.setTextColor(this.getColor(color.text_secondary));
      leftAxis.setTextSize(9.0F);
      leftAxis.setValueFormatter(new ValueFormatter() {
         public String getAxisLabel(float value, AxisBase axis) {
            return String.valueOf((int)value);
         }
      });
      chart.getAxisRight().setEnabled(false);
      XAxis xAxis = chart.getXAxis();
      xAxis.setPosition(XAxisPosition.BOTTOM);
      xAxis.setDrawGridLines(false);
      xAxis.setDrawAxisLine(true);
      xAxis.setAxisLineColor(this.getColor(color.candy_grid));
      xAxis.setGranularity(1.0F);
      xAxis.setGranularityEnabled(true);
      xAxis.setTextColor(this.getColor(color.text_secondary));
      xAxis.setTextSize(9.0F);
      xAxis.setYOffset(4.0F);
   }

   private void updateBudgetPanel() {
      double budget = this.parseAmount(this.binding.etBudget.getText().toString());
      this.binding.tvTotalBudget.setText(this.getString(string.currency_format, new Object[]{this.formatAmount(budget)}));
      if (this.emptyState) {
         this.binding.panelBudget.setBackgroundResource(drawable.bg_gradient_blue);
         this.binding.tvRemainLabel.setText(string.label_remaining);
         this.binding.tvRemainLabel.setTextColor(this.getColor(color.text_secondary));
         this.binding.tvRemainValue.setTextColor(this.getColor(color.color_primary));
         this.binding.tvRemainValue.setText(string.main_entry_empty);
      } else {
         double remaining = budget - this.monthExpense;
         if (remaining >= (double)0.0F) {
            this.binding.panelBudget.setBackgroundResource(drawable.bg_gradient_blue);
            this.binding.tvRemainLabel.setText(string.label_remaining);
            this.binding.tvRemainLabel.setTextColor(this.getColor(color.text_secondary));
            this.binding.tvRemainValue.setTextColor(this.getColor(color.color_primary));
            this.binding.tvRemainValue.setText(this.getString(string.currency_format, new Object[]{this.formatAmount(remaining)}));
         } else {
            this.binding.panelBudget.setBackgroundResource(drawable.bg_gradient_red);
            this.binding.tvRemainLabel.setText(string.label_remaining);
            this.binding.tvRemainLabel.setTextColor(this.getColor(color.color_warning));
            this.binding.tvRemainValue.setTextColor(this.getColor(color.color_warning));
            this.binding.tvRemainValue.setText(this.getString(string.label_over) + " " + this.getString(string.currency_format, new Object[]{this.formatAmount(-remaining)}));
         }

      }
   }

   private int parseDayOfMonth(String date) {
      if (date != null && date.length() >= 10) {
         try {
            return Integer.parseInt(date.substring(8, 10));
         } catch (NumberFormatException var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private double parseAmount(String text) {
      if (text != null && !text.trim().isEmpty()) {
         try {
            return Double.parseDouble(text.trim());
         } catch (NumberFormatException var3) {
            return (double)0.0F;
         }
      } else {
         return (double)0.0F;
      }
   }

   private String formatAmount(double amount) {
      return String.format(Locale.CHINA, "%.2f", amount);
   }
}
