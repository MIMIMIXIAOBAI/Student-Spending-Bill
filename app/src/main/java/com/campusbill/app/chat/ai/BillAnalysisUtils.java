package com.campusbill.app.chat.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.campusbill.app.data.BillEntity;
import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.DBHelper;
import com.campusbill.app.data.MerchantAmount;

import java.util.Collections;
import java.util.List;

/**
 * 账单分析工具：统一从本机 Room 数据库读取数据并计算。
 * 所有数据库操作均交给 DBHelper 的子线程执行，结果回到主线程回调。
 */
public final class BillAnalysisUtils {

    private static final String PREF_NAME = "bill_settings";
    private static final String KEY_BUDGET = "monthly_budget";

    private BillAnalysisUtils() {
    }

    /** 指定月份的总支出 */
    public static void getMonthTotalExpense(Context context, String yearMonth,
                                            DBHelper.DataCallback<Double> callback) {
        DBHelper.getInstance(context).sumAmountByMonthAndType(
                yearMonth, BillEntity.TYPE_EXPENSE, callback);
    }

    /** 指定日期的总支出 */
    public static void getDayTotalExpense(Context context, String date,
                                          DBHelper.DataCallback<Double> callback) {
        DBHelper.getInstance(context).getBillsByDate(date,
                new DBHelper.DataCallback<List<BillEntity>>() {
                    @Override
                    public void onSuccess(List<BillEntity> bills) {
                        double total = 0;
                        if (bills != null) {
                            for (BillEntity bill : bills) {
                                if (bill.type == BillEntity.TYPE_EXPENSE) {
                                    total += bill.amount;
                                }
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(total);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
    }

    /** 当前预算剩余金额（预算 - 指定月份总支出） */
    public static void getBudgetRemain(Context context, String yearMonth,
                                       DBHelper.DataCallback<Double> callback) {
        getMonthTotalExpense(context, yearMonth, new DBHelper.DataCallback<Double>() {
            @Override
            public void onSuccess(Double expense) {
                double budget = getBudget(context);
                if (callback != null) {
                    callback.onSuccess(budget - (expense == null ? 0 : expense));
                }
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /** 指定月份的分类金额排行（金额降序） */
    public static void getCategoryRank(Context context, String yearMonth,
                                       DBHelper.DataCallback<List<CategoryAmount>> callback) {
        DBHelper.getInstance(context).sumAmountByCategory(
                yearMonth, BillEntity.TYPE_EXPENSE, callback);
    }

    /** 指定月份商户消费 TopN（金额降序） */
    public static void getMerchantTop(Context context, String yearMonth, int topN,
                                      DBHelper.DataCallback<List<MerchantAmount>> callback) {
        DBHelper.getInstance(context).sumAmountByMerchant(
                yearMonth, BillEntity.TYPE_EXPENSE,
                new DBHelper.DataCallback<List<MerchantAmount>>() {
                    @Override
                    public void onSuccess(List<MerchantAmount> list) {
                        if (list == null) {
                            list = Collections.emptyList();
                        }
                        List<MerchantAmount> top =
                                list.size() > topN ? list.subList(0, topN) : list;
                        if (callback != null) {
                            callback.onSuccess(top);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                });
    }

    /** 读取本机保存的月度预算，未设置返回 0 */
    public static double getBudget(Context context) {
        return prefs(context).getFloat(KEY_BUDGET, 0f);
    }

    /** 保存月度预算到本机 */
    public static void saveBudget(Context context, double budget) {
        prefs(context).edit().putFloat(KEY_BUDGET, (float) budget).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
