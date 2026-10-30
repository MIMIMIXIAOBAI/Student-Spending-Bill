package com.campusbill.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.campusbill.app.R.color;
import com.campusbill.app.R.string;
import com.campusbill.app.data.BillEntity;
import com.campusbill.app.databinding.ItemHistoryBatchBinding;
import com.campusbill.app.databinding.ItemHistoryBillBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
   private static final int TYPE_BATCH = 0;
   private static final int TYPE_BILL = 1;
   private final List<Item> items = new ArrayList();
   private final Listener listener;

   public HistoryAdapter(Listener listener) {
      this.listener = listener;
   }

   public void setItems(List<Item> data) {
      this.items.clear();
      if (data != null) {
         this.items.addAll(data);
      }

      this.notifyDataSetChanged();
   }

   public int getItemViewType(int position) {
      return ((Item)this.items.get(position)).type;
   }

   @NonNull
   public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      return (RecyclerView.ViewHolder)(viewType == 0 ? new BatchHolder(ItemHistoryBatchBinding.inflate(inflater, parent, false)) : new BillHolder(ItemHistoryBillBinding.inflate(inflater, parent, false)));
   }

   public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
      Item item = (Item)this.items.get(position);
      if (holder instanceof BatchHolder) {
         this.bindBatch((BatchHolder)holder, item.batch);
      } else {
         this.bindBill((BillHolder)holder, item.bill);
      }

   }

   public int getItemCount() {
      return this.items.size();
   }

   private void bindBatch(BatchHolder holder, Batch batch) {
      holder.binding.tvBatchName.setText(batch.name);
      holder.binding.tvBatchCount.setText(holder.itemView.getContext().getString(string.history_batch_count, new Object[]{batch.count}));
      holder.binding.tvBatchMeta.setText(batch.meta);
      holder.binding.btnClearBatch.setOnClickListener((v) -> this.listener.onClearBatch(batch));
   }

   private void bindBill(BillHolder holder, BillEntity bill) {
      ItemHistoryBillBinding b = holder.binding;
      String time = bill.time == null ? "" : bill.time;
      if (time.length() > 5) {
         time = time.substring(0, 5);
      }

      String dateText = (bill.date == null ? "" : bill.date) + "  " + time;
      b.tvDate.setText(dateText);
      String category = bill.category != null && !bill.category.trim().isEmpty() ? bill.category.trim() : "其他";
      b.tvCategory.setText(bill.isIncome() ? "收入 · " + category : category);
      String merchant = bill.merchant == null ? "" : bill.merchant.trim();
      if (merchant.isEmpty()) {
         merchant = bill.remark == null ? "" : bill.remark.trim();
      }

      b.tvMerchant.setText(merchant.isEmpty() ? "—" : merchant);
      b.tvMerchant.setVisibility(0);
      b.tvAmount.setText(String.format(Locale.CHINA, "%s¥%.2f", bill.isIncome() ? "+" : "-", bill.amount));
      b.tvAmount.setTextColor(ContextCompat.getColor(b.getRoot().getContext(), bill.isIncome() ? color.color_income : color.color_warning));
      b.btnEdit.setOnClickListener((v) -> this.listener.onEdit(bill));
      b.btnDelete.setOnClickListener((v) -> this.listener.onDelete(bill));
   }

   public static class Batch {
      public long batchId;
      public String name;
      public String meta;
      public int count;
   }

   public static class Item {
      public int type;
      public Batch batch;
      public BillEntity bill;
   }

   private static final class BatchHolder extends RecyclerView.ViewHolder {
      final ItemHistoryBatchBinding binding;

      BatchHolder(ItemHistoryBatchBinding binding) {
         super(binding.getRoot());
         this.binding = binding;
      }
   }

   private static final class BillHolder extends RecyclerView.ViewHolder {
      final ItemHistoryBillBinding binding;

      BillHolder(ItemHistoryBillBinding binding) {
         super(binding.getRoot());
         this.binding = binding;
      }
   }

   public interface Listener {
      void onClearBatch(Batch batch);

      void onEdit(BillEntity bill);

      void onDelete(BillEntity bill);
   }
}
