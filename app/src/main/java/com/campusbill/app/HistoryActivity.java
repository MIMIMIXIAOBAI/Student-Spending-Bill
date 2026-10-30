package com.campusbill.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat.Type;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.campusbill.app.R.color;
import com.campusbill.app.R.drawable;
import com.campusbill.app.R.string;
import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.ImportBatchEntity;
import com.campusbill.app.databinding.ActivityHistoryBinding;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {
   private static final DateTimeFormatter BATCH_TIME_FORMAT;
   private static final int FILTER_ALL = -1;
   private ActivityHistoryBinding binding;
   private HistoryAdapter adapter;
   private List<ImportBatchEntity> batches = new ArrayList();
   private List<BillEntity> allBills = new ArrayList();
   private String searchText = "";
   private int typeFilter = -1;
   private String monthFilter;
   private int totalBills;
   private boolean resumedOnce;

   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      this.binding = ActivityHistoryBinding.inflate(this.getLayoutInflater());
      this.setContentView(this.binding.getRoot());
      this.binding.btnBack.setOnClickListener((v) -> this.finish());
      this.adapter = new HistoryAdapter(new HistoryAdapter.Listener() {
         public void onClearBatch(HistoryAdapter.Batch batch) {
            HistoryActivity.this.confirmClearBatch(batch);
         }

         public void onEdit(BillEntity bill) {
            HistoryActivity.this.openEdit(bill);
         }

         public void onDelete(BillEntity bill) {
            HistoryActivity.this.confirmDelete(bill);
         }
      });
      this.binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
      this.binding.rvHistory.setAdapter(this.adapter);
      this.binding.btnClearAll.setOnClickListener((v) -> this.confirmClearAll());
      this.applyBottomInset();
      this.setupFilters();
      this.loadHistory();
      this.binding.tvHistorySummary.setText(string.history_summary_empty);
   }

   protected void onResume() {
      super.onResume();
      if (this.resumedOnce) {
         this.loadHistory();
      }

      this.resumedOnce = true;
   }

   private void applyBottomInset() {
      int side = (int)(16.0F * this.getResources().getDisplayMetrics().density);
      int baseBottom = (int)(24.0F * this.getResources().getDisplayMetrics().density);
      ViewCompat.setOnApplyWindowInsetsListener(this.binding.historyRoot, (v, insets) -> {
         int bottom = insets.getInsets(Type.navigationBars()).bottom;
         this.binding.rvHistory.setPadding(side, (int)(4.0F * this.getResources().getDisplayMetrics().density), side, baseBottom + bottom);
         return insets;
      });
   }

   private void setupFilters() {
      this.binding.chipAll.setOnClickListener((v) -> this.setTypeFilter(-1));
      this.binding.chipExpense.setOnClickListener((v) -> this.setTypeFilter(0));
      this.binding.chipIncome.setOnClickListener((v) -> this.setTypeFilter(1));
      this.binding.tvMonthFilter.setOnClickListener((v) -> this.showMonthPicker());
      this.binding.etSearch.addTextChangedListener(new TextWatcher() {
         public void beforeTextChanged(CharSequence s, int start, int count, int after) {
         }

         public void onTextChanged(CharSequence s, int start, int before, int count) {
         }

         public void afterTextChanged(Editable s) {
            HistoryActivity.this.searchText = s.toString().trim();
            HistoryActivity.this.applyFilters();
         }
      });
      this.binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
         InputMethodManager imm = (InputMethodManager)this.getSystemService("input_method");
         if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
         }

         v.clearFocus();
         return true;
      });
      this.updateTypeChips();
      this.updateMonthChip();
   }

   private void setTypeFilter(int type) {
      this.typeFilter = type;
      this.updateTypeChips();
      this.applyFilters();
   }

   private void updateTypeChips() {
      this.applyChipStyle(this.binding.chipAll, this.typeFilter == -1);
      this.applyChipStyle(this.binding.chipExpense, this.typeFilter == 0);
      this.applyChipStyle(this.binding.chipIncome, this.typeFilter == 1);
   }

   private void updateMonthChip() {
      this.binding.tvMonthFilter.setText(this.monthFilter == null ? this.getString(string.history_filter_all_months) : this.monthLabel(this.monthFilter));
      this.applyChipStyle(this.binding.tvMonthFilter, this.monthFilter != null);
   }

   private void applyChipStyle(TextView chip, boolean selected) {
      if (selected) {
         chip.setBackgroundResource(drawable.bg_y2k_pill);
         chip.setTextColor(ContextCompat.getColor(this, color.candy_ink));
         chip.setTypeface(Typeface.DEFAULT_BOLD);
      } else {
         chip.setBackgroundResource(drawable.bg_y2k_chip);
         chip.setTextColor(ContextCompat.getColor(this, color.y2k_violet));
         chip.setTypeface(Typeface.DEFAULT);
      }

   }

   private void showMonthPicker() {
      List<String> months = this.collectMonths();
      String[] labels = new String[months.size() + 1];
      labels[0] = this.getString(string.history_filter_all_months);

      for(int i = 0; i < months.size(); ++i) {
         labels[i + 1] = this.monthLabel((String)months.get(i));
      }

      int checked = this.monthFilter == null ? 0 : months.indexOf(this.monthFilter) + 1;
      (new AlertDialog.Builder(this)).setTitle(string.history_filter_month_title).setSingleChoiceItems(labels, checked, (dialog, which) -> {
         this.monthFilter = which == 0 ? null : (String)months.get(which - 1);
         this.updateMonthChip();
         this.applyFilters();
         dialog.dismiss();
      }).setNegativeButton(string.btn_cancel, (DialogInterface.OnClickListener)null).show();
   }

   private List<String> collectMonths() {
      Set<String> set = new HashSet();

      for(BillEntity bill : this.allBills) {
         String month = this.billMonth(bill);
         if (month != null) {
            set.add(month);
         }
      }

      List<String> months = new ArrayList(set);
      Collections.sort(months, Collections.reverseOrder());
      return months;
   }

   private String billMonth(BillEntity bill) {
      return bill.date != null && bill.date.length() >= 7 ? bill.date.substring(0, 7) : null;
   }

   private String monthLabel(String yearMonth) {
      try {
         String[] parts = yearMonth.split("-");
         return parts[0] + "年" + Integer.parseInt(parts[1]) + "月";
      } catch (Exception var3) {
         return yearMonth;
      }
   }

   private void applyFilters() {
      if (this.allBills.isEmpty()) {
         this.renderEmpty();
      } else {
         List<BillEntity> filtered = new ArrayList();
         String keyword = this.searchText.toLowerCase(Locale.CHINA);
         boolean hasFilter = !keyword.isEmpty() || this.typeFilter != -1 || this.monthFilter != null;

         for(BillEntity bill : this.allBills) {
            if ((this.typeFilter == -1 || bill.type == this.typeFilter) && (this.monthFilter == null || this.monthFilter.equals(this.billMonth(bill))) && (keyword.isEmpty() || this.matchesKeyword(bill, keyword))) {
               filtered.add(bill);
            }
         }

         this.buildItems(filtered, hasFilter);
      }
   }

   private boolean matchesKeyword(BillEntity bill, String keyword) {
      StringBuilder text = new StringBuilder();
      text.append(bill.category == null ? "" : bill.category).append(' ').append(bill.merchant == null ? "" : bill.merchant).append(' ').append(bill.remark == null ? "" : bill.remark).append(' ').append(bill.date == null ? "" : bill.date).append(' ').append(String.format(Locale.CHINA, "%.2f", bill.amount)).append(' ').append(this.getString(bill.type == 1 ? string.history_filter_income : string.history_filter_expense));
      return text.toString().toLowerCase(Locale.CHINA).contains(keyword);
   }

   private void loadHistory() {
      DBHelper.getInstance(this).getBatches(new DBHelper.DataCallback<List<ImportBatchEntity>>() {
         public void onSuccess(List<ImportBatchEntity> data) {
            HistoryActivity.this.batches = (List<ImportBatchEntity>)(data == null ? new ArrayList() : data);
            HistoryActivity.this.loadBills();
         }

         public void onError(Exception e) {
            Toast.makeText(HistoryActivity.this, string.history_load_failed, 0).show();
            HistoryActivity.this.renderEmpty();
         }
      });
   }

   private void loadBills() {
      DBHelper.getInstance(this).getAllBills(new DBHelper.DataCallback<List<BillEntity>>() {
         public void onSuccess(List<BillEntity> data) {
            HistoryActivity.this.allBills = (List<BillEntity>)(data == null ? new ArrayList() : data);
            HistoryActivity.this.applyFilters();
         }

         public void onError(Exception e) {
            Toast.makeText(HistoryActivity.this, string.history_load_failed, 0).show();
            HistoryActivity.this.renderEmpty();
         }
      });
   }

   private void buildItems(List<BillEntity> bills, boolean filtered) {
      Map<Long, List<BillEntity>> groups = new LinkedHashMap();

      for(BillEntity bill : bills) {
         List<BillEntity> group = (List)groups.get(bill.batchId);
         if (group == null) {
            group = new ArrayList();
            groups.put(bill.batchId, group);
         }

         group.add(bill);
      }

      List<HistoryAdapter.Item> items = new ArrayList();
      int batchCount = 0;

      for(ImportBatchEntity batch : this.batches) {
         List<BillEntity> group = (List)groups.remove(batch.id);
         if (group != null && !group.isEmpty()) {
            items.add(this.batchHeader(batch.id, batch.name, batch.source + " · " + this.formatBatchTime(batch.createTime), group.size()));

            for(BillEntity bill : group) {
               items.add(this.billItem(bill));
            }

            ++batchCount;
         }
      }

      List<BillEntity> manual = (List)groups.remove(0L);
      if (manual != null && !manual.isEmpty()) {
         items.add(this.batchHeader(0L, this.getString(string.history_batch_legacy), this.getString(string.history_batch_legacy_meta), manual.size()));

         for(BillEntity bill : manual) {
            items.add(this.billItem(bill));
         }

         ++batchCount;
      }

      for(Map.Entry<Long, List<BillEntity>> entry : groups.entrySet()) {
         List<BillEntity> group = (List)entry.getValue();
         if (!group.isEmpty()) {
            items.add(this.batchHeader((Long)entry.getKey(), this.getString(string.history_batch_unknown), this.getString(string.history_batch_unknown_meta), group.size()));

            for(BillEntity bill : group) {
               items.add(this.billItem(bill));
            }

            ++batchCount;
         }
      }

      this.adapter.setItems(items);
      this.totalBills = this.allBills.size();
      if (filtered) {
         this.binding.tvHistorySummary.setText(this.getString(string.history_filtered_summary, new Object[]{bills.size(), batchCount}));
      } else {
         this.binding.tvHistorySummary.setText(this.getString(string.history_summary, new Object[]{this.totalBills, batchCount}));
      }

      boolean empty = items.isEmpty();
      this.binding.emptyContainer.setVisibility(empty ? 0 : 8);
      this.binding.rvHistory.setVisibility(empty ? 8 : 0);
      if (empty) {
         this.setEmptyText(filtered);
      }

      this.binding.btnClearAll.setEnabled(this.totalBills > 0);
      this.binding.btnClearAll.setAlpha(this.totalBills > 0 ? 1.0F : 0.45F);
   }

   private void setEmptyText(boolean noMatch) {
      this.binding.tvEmptyTitle.setText(noMatch ? string.history_no_match_title : string.history_empty_title);
      this.binding.tvEmptyDesc.setText(noMatch ? string.history_no_match_desc : string.history_empty_desc);
   }

   private HistoryAdapter.Item batchHeader(long batchId, String name, String meta, int count) {
      HistoryAdapter.Batch batch = new HistoryAdapter.Batch();
      batch.batchId = batchId;
      batch.name = name != null && !name.trim().isEmpty() ? name : this.getString(string.history_batch_unknown);
      batch.meta = meta;
      batch.count = count;
      HistoryAdapter.Item item = new HistoryAdapter.Item();
      item.type = 0;
      item.batch = batch;
      return item;
   }

   private HistoryAdapter.Item billItem(BillEntity bill) {
      HistoryAdapter.Item item = new HistoryAdapter.Item();
      item.type = 1;
      item.bill = bill;
      return item;
   }

   private String formatBatchTime(long millis) {
      return millis <= 0L ? "" : BATCH_TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
   }

   private void renderEmpty() {
      this.allBills = new ArrayList();
      this.adapter.setItems(new ArrayList());
      this.totalBills = 0;
      this.binding.tvHistorySummary.setText(string.history_summary_empty);
      this.binding.emptyContainer.setVisibility(0);
      this.binding.rvHistory.setVisibility(8);
      this.setEmptyText(false);
      this.binding.btnClearAll.setEnabled(false);
      this.binding.btnClearAll.setAlpha(0.45F);
   }

   private void openEdit(BillEntity bill) {
      this.startActivity((new Intent(this, EditBillActivity.class)).putExtra("extra_bill_id", bill.id));
   }

   private void confirmDelete(BillEntity bill) {
      (new AlertDialog.Builder(this)).setTitle(string.bill_delete_confirm_title).setMessage(string.history_delete_confirm).setNegativeButton(string.btn_cancel, (DialogInterface.OnClickListener)null).setPositiveButton(string.bill_delete, (dialog, which) -> this.deleteBill(bill)).show();
   }

   private void deleteBill(BillEntity bill) {
      DBHelper.getInstance(this).deleteById(bill.id, new DBHelper.DataCallback<Integer>() {
         public void onSuccess(Integer rows) {
            Toast.makeText(HistoryActivity.this, string.bill_deleted, 0).show();
            HistoryActivity.this.loadHistory();
         }

         public void onError(Exception e) {
            Toast.makeText(HistoryActivity.this, string.bill_delete_failed, 0).show();
         }
      });
   }

   private void confirmClearBatch(HistoryAdapter.Batch batch) {
      (new AlertDialog.Builder(this)).setTitle(string.history_clear_batch_title).setMessage(this.getString(string.history_clear_batch_confirm, new Object[]{batch.name, batch.count})).setNegativeButton(string.btn_cancel, (DialogInterface.OnClickListener)null).setPositiveButton(string.bill_delete, (dialog, which) -> this.clearBatch(batch)).show();
   }

   private void clearBatch(HistoryAdapter.Batch batch) {
      DBHelper.getInstance(this).deleteBatchWithBills(batch.batchId, new DBHelper.DataCallback<Integer>() {
         public void onSuccess(Integer rows) {
            int count = rows == null ? 0 : rows;
            Toast.makeText(HistoryActivity.this, HistoryActivity.this.getString(string.history_batch_cleared, new Object[]{count}), 0).show();
            HistoryActivity.this.loadHistory();
         }

         public void onError(Exception e) {
            Toast.makeText(HistoryActivity.this, string.bill_delete_failed, 0).show();
         }
      });
   }

   private void confirmClearAll() {
      if (this.totalBills > 0) {
         (new AlertDialog.Builder(this)).setTitle(string.history_clear_all_title).setMessage(this.getString(string.history_clear_all_confirm, new Object[]{this.totalBills})).setNegativeButton(string.btn_cancel, (DialogInterface.OnClickListener)null).setPositiveButton(string.bill_delete, (dialog, which) -> this.clearAll()).show();
      }
   }

   private void clearAll() {
      DBHelper.getInstance(this).clearHistory(new DBHelper.DataCallback<Integer>() {
         public void onSuccess(Integer rows) {
            int count = rows == null ? 0 : rows;
            Toast.makeText(HistoryActivity.this, HistoryActivity.this.getString(string.history_all_cleared, new Object[]{count}), 0).show();
            HistoryActivity.this.loadHistory();
         }

         public void onError(Exception e) {
            Toast.makeText(HistoryActivity.this, string.bill_delete_failed, 0).show();
         }
      });
   }

   static {
      BATCH_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.CHINA);
   }
}
