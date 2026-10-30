package com.campusbill.app;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusbill.app.data.MerchantAmount;
import com.campusbill.app.databinding.ItemMerchantRankBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 商户消费排行列表：按金额降序，前三名放大字号并使用金色 / 银色 / 铜色名次徽标。
 */
public class MerchantRankAdapter extends RecyclerView.Adapter<MerchantRankAdapter.Holder> {

    private static final int COLOR_GOLD = 0xFFFFF0C2;
    private static final int COLOR_SILVER = 0xFFE7EEF8;
    private static final int COLOR_BRONZE = 0xFFFFE0C9;

    private final List<MerchantAmount> items = new ArrayList<>();
    private double total;
    private double maxAmount = 1;

    public void setData(List<MerchantAmount> data, double total) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        this.total = total;
        this.maxAmount = 1;
        if (!items.isEmpty()) {
            maxAmount = Math.max(1, items.get(0).total);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMerchantRankBinding binding = ItemMerchantRankBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        MerchantAmount item = items.get(position);
        int rank = position + 1;
        boolean top = rank <= 3;

        holder.binding.tvRank.setText(String.format(Locale.CHINA, "%d", rank));
        if (top) {
            holder.binding.tvRank.setBackgroundResource(R.drawable.bg_rank_badge);
            int badge = rank == 1 ? COLOR_GOLD : rank == 2 ? COLOR_SILVER : COLOR_BRONZE;
            holder.binding.tvRank.getBackground().mutate().setTint(badge);
            holder.binding.tvRank.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            holder.binding.tvMerchantName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
            holder.binding.tvMerchantName.setTypeface(Typeface.DEFAULT_BOLD);
            holder.binding.tvMerchantAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            holder.binding.tvMerchantAmount.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            holder.binding.tvRank.setBackgroundResource(R.drawable.bg_rank_badge_normal);
            holder.binding.tvRank.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            holder.binding.tvMerchantName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            holder.binding.tvMerchantName.setTypeface(Typeface.DEFAULT);
            holder.binding.tvMerchantAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
            holder.binding.tvMerchantAmount.setTypeface(Typeface.DEFAULT);
        }

        holder.binding.tvMerchantName.setText(item.merchant);
        holder.binding.tvMerchantAmount.setText(
                holder.binding.getRoot().getContext().getString(
                        R.string.currency_format,
                        String.format(Locale.CHINA, "%.2f", item.total)));

        double percent = total > 0 ? item.total / total * 100 : 0;
        String percentText;
        if (percent >= 1) {
            percentText = String.format(Locale.CHINA, "%.0f%%", percent);
        } else if (percent >= 0.1) {
            percentText = String.format(Locale.CHINA, "%.1f%%", percent);
        } else {
            percentText = holder.binding.getRoot().getContext()
                    .getString(R.string.merchant_rank_percent_min);
        }
        holder.binding.tvMerchantPercent.setText(percentText);

        int progress = (int) Math.round(1000 * item.total / maxAmount);
        holder.binding.progressShare.setProgress(Math.max(1, Math.min(1000, progress)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemMerchantRankBinding binding;

        Holder(ItemMerchantRankBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
