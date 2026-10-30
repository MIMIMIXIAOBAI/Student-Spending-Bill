package com.campusbill.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.MerchantAmount;
import com.campusbill.app.databinding.ActivityMerchantRankBinding;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户消费排行：按所选月份汇总各商户消费金额，从高到低展示。
 * 数据来自本机 Room 数据库（支出账单）。
 */
public class MerchantRankActivity extends AppCompatActivity {

    public static final String EXTRA_MONTH = "extra_month";

    private ActivityMerchantRankBinding binding;
    private MerchantRankAdapter adapter;
    private YearMonth month;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMerchantRankBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnBack.setOnClickListener(v -> finish());

        String monthExtra = getIntent().getStringExtra(EXTRA_MONTH);
        month = monthExtra == null ? YearMonth.now() : parseMonth(monthExtra);

        adapter = new MerchantRankAdapter();
        binding.rvMerchantRank.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMerchantRank.setAdapter(adapter);

        loadRank();
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    /** 从 Room 读取当月支出按商户汇总，合并未标注商户后按金额降序 */
    private void loadRank() {
        DBHelper.getInstance(this).sumAmountByMerchant(month.toString(),
                BillEntity.TYPE_EXPENSE, new DBHelper.DataCallback<List<MerchantAmount>>() {
                    @Override
                    public void onSuccess(List<MerchantAmount> data) {
                        List<MerchantAmount> merged = mergeUnknownMerchant(data);
                        Collections.sort(merged, (a, b) -> Double.compare(b.total, a.total));

                        double total = 0;
                        for (MerchantAmount item : merged) {
                            total += item.total;
                        }
                        adapter.setData(merged, total);
                        binding.tvRankSummary.setText(getString(
                                R.string.merchant_rank_subtitle_format,
                                getString(R.string.month_picker_label_format,
                                        month.getYear(), month.getMonthValue()),
                                merged.size()));
                        binding.tvRankEmpty.setVisibility(
                                merged.isEmpty() ? View.VISIBLE : View.GONE);
                        binding.rvMerchantRank.setVisibility(
                                merged.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onError(Exception e) {
                        adapter.setData(Collections.emptyList(), 0);
                        binding.tvRankSummary.setText(getString(
                                R.string.merchant_rank_subtitle_format,
                                getString(R.string.month_picker_label_format,
                                        month.getYear(), month.getMonthValue()), 0));
                        binding.tvRankEmpty.setVisibility(View.VISIBLE);
                        binding.rvMerchantRank.setVisibility(View.GONE);
                    }
                });
    }

    /** 商户名为空或未识别的记录统一归入「未标注商户」，并合并金额 */
    private List<MerchantAmount> mergeUnknownMerchant(List<MerchantAmount> data) {
        Map<String, Double> sums = new LinkedHashMap<>();
        if (data != null) {
            for (MerchantAmount item : data) {
                String name = item.merchant;
                if (name == null || name.trim().isEmpty()) {
                    name = getString(R.string.merchant_rank_unknown);
                }
                Double old = sums.get(name);
                sums.put(name, old == null ? item.total : old + item.total);
            }
        }
        List<MerchantAmount> merged = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sums.entrySet()) {
            MerchantAmount item = new MerchantAmount();
            item.merchant = entry.getKey();
            item.total = entry.getValue();
            merged.add(item);
        }
        return merged;
    }
}
