package com.campusbill.app.chat.ai;

import android.content.Context;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.MerchantAmount;
import com.campusbill.app.data.SelectedMonthStore;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * 账单上下文构建器：联网请求远程 AI 前，从本机 Room 数据库读取账单关键数据，
 * 汇总成一段结构化的「账单快照」文本，注入 system prompt。
 * 远程 AI 据此基于用户真实账单作答，而不是凭空编造。
 * 全部数据库读取在 DBHelper 子线程执行，结果回主线程。
 */
public final class BillContextBuilder {

    /** 注入给远程 AI 的最大近期流水条数，避免 prompt 过长 */
    private static final int MAX_RECENT_BILLS = 15;
    /** 分类 / 商户展示的条数上限 */
    private static final int MAX_TOP = 5;

    private BillContextBuilder() {
    }

    public interface Callback {
        void onReady(String contextText);
    }

    /**
     * 异步构建账单快照文本。无论数据库是否有数据都会回调 onReady，
     * 数据为空时返回「暂无账单记录」的说明，保证远程请求不被阻塞。
     */
    public static void build(Context context, Callback callback) {
        build(context, SelectedMonthStore.get(context).toString(), callback);
    }

    public static void build(Context context, String month, Callback callback) {
        final String selected = month == null || month.trim().isEmpty()
                ? SelectedMonthStore.get(context).toString()
                : month.trim();
        DBHelper db = DBHelper.getInstance(context);

        db.sumAmountByMonthAndType(selected, BillEntity.TYPE_EXPENSE,
                new DBHelper.DataCallback<Double>() {
                    @Override
                    public void onSuccess(Double expense) {
                        double totalExpense = expense == null ? 0 : expense;
                        fetchIncome(context, db, selected, totalExpense, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onReady(fallback());
                    }
                });
    }

    private static void fetchIncome(Context context, DBHelper db, String month,
                                    double totalExpense, Callback callback) {
        db.sumAmountByMonthAndType(month, BillEntity.TYPE_INCOME,
                new DBHelper.DataCallback<Double>() {
                    @Override
                    public void onSuccess(Double income) {
                        double totalIncome = income == null ? 0 : income;
                        fetchCategories(context, db, month, totalExpense, totalIncome, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onReady(fallback());
                    }
                });
    }

    private static void fetchCategories(Context context, DBHelper db, String month,
                                        double totalExpense, double totalIncome,
                                        Callback callback) {
        db.sumAmountByCategory(month, BillEntity.TYPE_EXPENSE,
                new DBHelper.DataCallback<List<CategoryAmount>>() {
                    @Override
                    public void onSuccess(List<CategoryAmount> categories) {
                        fetchMerchants(context, db, month, totalExpense, totalIncome,
                                categories, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onReady(fallback());
                    }
                });
    }

    private static void fetchMerchants(Context context, DBHelper db, String month,
                                       double totalExpense, double totalIncome,
                                       List<CategoryAmount> categories, Callback callback) {
        db.sumAmountByMerchant(month, BillEntity.TYPE_EXPENSE,
                new DBHelper.DataCallback<List<MerchantAmount>>() {
                    @Override
                    public void onSuccess(List<MerchantAmount> merchants) {
                        fetchRecent(context, db, month, totalExpense, totalIncome,
                                categories, merchants, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onReady(fallback());
                    }
                });
    }

    private static void fetchRecent(Context context, DBHelper db, String month,
                                    double totalExpense, double totalIncome,
                                    List<CategoryAmount> categories,
                                    List<MerchantAmount> merchants, Callback callback) {
        db.getBillsByMonth(month, new DBHelper.DataCallback<List<BillEntity>>() {
            @Override
            public void onSuccess(List<BillEntity> bills) {
                double budget = BillAnalysisUtils.getBudget(context);
                callback.onReady(assemble(month, totalExpense, totalIncome, budget,
                        categories, merchants, bills));
            }

            @Override
            public void onError(Exception e) {
                callback.onReady(fallback());
            }
        });
    }

    /** 把各项数据拼装成给远程 AI 阅读的快照文本 */
    private static String assemble(String month, double totalExpense, double totalIncome,
                                   double budget, List<CategoryAmount> categories,
                                   List<MerchantAmount> merchants, List<BillEntity> bills) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是用户选定月份 ").append(month).append(" 的真实账单数据，请基于它回答。\n");
        sb.append("当用户说「当月」「本月」「这个月」时，都是指 ")
                .append(month).append("，不是手机日历上的当前月。\n");
        sb.append(String.format(Locale.CHINA, "%s 总支出：%.2f 元；总收入：%.2f 元。\n",
                month, totalExpense, totalIncome));
        if (budget > 0) {
            double remain = budget - totalExpense;
            sb.append(String.format(Locale.CHINA, "本月预算：%.2f 元，剩余：%.2f 元%s。\n",
                    budget, Math.abs(remain), remain < 0 ? "（已超支）" : ""));
        }

        if (categories != null && !categories.isEmpty()) {
            sb.append("支出分类（金额降序）：");
            int n = Math.min(categories.size(), MAX_TOP);
            for (int i = 0; i < n; i++) {
                CategoryAmount c = categories.get(i);
                sb.append(String.format(Locale.CHINA, "%s %.2f 元", c.category, c.total));
                sb.append(i < n - 1 ? "；" : "。\n");
            }
        }

        if (merchants != null && !merchants.isEmpty()) {
            sb.append("高频消费商户：");
            int n = Math.min(merchants.size(), MAX_TOP);
            for (int i = 0; i < n; i++) {
                MerchantAmount m = merchants.get(i);
                sb.append(String.format(Locale.CHINA, "%s %.2f 元", m.merchant, m.total));
                sb.append(i < n - 1 ? "；" : "。\n");
            }
        }

        if (bills != null && !bills.isEmpty()) {
            sb.append("近期账单（最多 ").append(MAX_RECENT_BILLS).append(" 条）：\n");
            int start = Math.max(0, bills.size() - MAX_RECENT_BILLS);
            for (int i = start; i < bills.size(); i++) {
                BillEntity b = bills.get(i);
                sb.append(String.format(Locale.CHINA, "- %s %s %s %.2f 元（%s）\n",
                        b.date, b.isIncome() ? "收入" : "支出",
                        b.category, b.amount,
                        b.merchant == null || b.merchant.isEmpty() ? "未填商户" : b.merchant));
            }
        }
        return sb.toString();
    }

    private static String fallback() {
        return "（暂时无法读取账单数据，请仅基于通用校园消费常识回答，不要编造具体金额。）";
    }
}
