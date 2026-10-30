package com.campusbill.app;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.databinding.ItemBillDayBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 当日账单列表：收入行淡绿色底、支出行淡红色底，右侧提供编辑 / 删除操作。
 */
public class DayBillAdapter extends RecyclerView.Adapter<DayBillAdapter.Holder> {

    public interface Listener {
        void onEdit(BillEntity bill);

        void onDelete(BillEntity bill);
    }

    private final List<BillEntity> items = new ArrayList<>();
    private final Listener listener;

    public DayBillAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<BillEntity> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillDayBinding binding = ItemBillDayBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BillEntity bill = items.get(position);
        boolean income = bill.isIncome();

        holder.binding.billRow.setBackgroundResource(income
                ? R.drawable.bg_bill_income : R.drawable.bg_bill_expense);
        holder.binding.tvBillCategory.setText(bill.category);
        holder.binding.tvBillAmount.setTextColor(ContextCompat.getColor(
                holder.binding.getRoot().getContext(),
                income ? R.color.color_income : R.color.color_warning));
        holder.binding.tvBillAmount.setText(String.format(Locale.CHINA, "%s¥%.2f",
                income ? "+" : "-", bill.amount));

        String merchant = bill.merchant;
        if (merchant == null || merchant.trim().isEmpty()) {
            merchant = holder.binding.getRoot().getContext()
                    .getString(R.string.bill_unknown_merchant);
        }
        holder.binding.tvBillMerchant.setText(merchant);
        holder.binding.tvBillTime.setText(bill.time == null ? "" : bill.time);

        holder.binding.tvBillEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(bill);
            }
        });
        holder.binding.tvBillDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(bill);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemBillDayBinding binding;

        Holder(ItemBillDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
