package com.campusbill.app.chat.ai;

import com.campusbill.app.data.CategoryAmount;
import com.campusbill.app.data.MerchantAmount;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * 回复生成器：按命中的意图与账单计算结果，套用 AiReplyConstants 中的模板拼接回复。
 */
public final class ReplyGenerator {

    private ReplyGenerator() {
    }

    /** 预算查询 */
    public static String budget(double budget, double expense) {
        if (budget <= 0) {
            return AiReplyConstants.BUDGET_UNSET;
        }
        double remaining = budget - expense;
        if (remaining >= 0) {
            return String.format(Locale.CHINA, AiReplyConstants.BUDGET_POSITIVE,
                    money(remaining), money(budget), money(expense));
        }
        return String.format(Locale.CHINA, AiReplyConstants.BUDGET_NEGATIVE,
                money(-remaining), money(budget), money(expense));
    }

    /** 本月支出总额 */
    public static String totalMonth(String yearMonth, double expense) {
        String label = monthLabel(yearMonth);
        if (expense <= 0) {
            return String.format(Locale.CHINA, AiReplyConstants.TOTAL_EMPTY, label);
        }
        return String.format(Locale.CHINA, AiReplyConstants.TOTAL_MONTH,
                label, money(expense));
    }

    /** 当日支出总额 */
    public static String totalDay(String date, double expense) {
        String label = dateLabel(date);
        if (expense <= 0) {
            return String.format(Locale.CHINA, AiReplyConstants.TOTAL_EMPTY, label);
        }
        return String.format(Locale.CHINA, AiReplyConstants.TOTAL_DAY, label, money(expense));
    }

    /** 分类排行：列出前 3 名并给出占比最高的分类 */
    public static String category(String yearMonth, List<CategoryAmount> list) {
        String label = monthLabel(yearMonth);
        if (list == null || list.isEmpty()) {
            return String.format(Locale.CHINA, AiReplyConstants.CATEGORY_EMPTY, label);
        }
        double total = 0;
        for (CategoryAmount item : list) {
            total += item.total;
        }
        StringBuilder ranking = new StringBuilder();
        int count = Math.min(3, list.size());
        for (int i = 0; i < count; i++) {
            CategoryAmount item = list.get(i);
            if (i > 0) {
                ranking.append('\n');
            }
            ranking.append(i + 1).append(". ").append(item.category)
                    .append(" ¥").append(money(item.total));
        }
        CategoryAmount top = list.get(0);
        String percent = total > 0
                ? String.format(Locale.CHINA, "%.1f", top.total / total * 100)
                : "0";
        return String.format(Locale.CHINA, AiReplyConstants.CATEGORY,
                label, ranking, top.category, percent);
    }

    /** 商户消费 TopN */
    public static String merchantTop(String yearMonth, List<MerchantAmount> list) {
        String label = monthLabel(yearMonth);
        if (list == null || list.isEmpty()) {
            return String.format(Locale.CHINA, AiReplyConstants.RANK_EMPTY, label);
        }
        StringBuilder ranking = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            MerchantAmount item = list.get(i);
            if (i > 0) {
                ranking.append('\n');
            }
            ranking.append(i + 1).append(". ").append(item.merchant)
                    .append(" ¥").append(money(item.total));
        }
        return String.format(Locale.CHINA, AiReplyConstants.RANK,
                label, list.size(), ranking);
    }

    /** 省钱建议：基于本月支出与最大分类，给出可执行的额度目标 */
    public static String advice(String yearMonth, double expense, List<CategoryAmount> list) {
        if (expense <= 0 || list == null || list.isEmpty()) {
            return AiReplyConstants.ADVICE_EMPTY;
        }
        CategoryAmount top = list.get(0);
        String percent = String.format(Locale.CHINA, "%.1f", top.total / expense * 100);
        double suggestedMonth = top.total * 0.9;
        double suggestedWeek = suggestedMonth / 4;
        return String.format(Locale.CHINA, AiReplyConstants.ADVICE,
                monthLabel(yearMonth), money(expense), top.category, percent,
                money(top.total), money(suggestedMonth), money(suggestedWeek));
    }

    public static String help() {
        return AiReplyConstants.HELP;
    }

    public static String fallback() {
        return AiReplyConstants.FALLBACK;
    }

    /** yyyy-MM → 2026年10月 */
    public static String monthLabel(String yearMonth) {
        try {
            YearMonth month = YearMonth.parse(yearMonth);
            return month.getYear() + "年" + month.getMonthValue() + "月";
        } catch (Exception e) {
            return yearMonth;
        }
    }

    /** yyyy-MM-dd → 10月31日 */
    public static String dateLabel(String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            return localDate.getMonthValue() + "月" + localDate.getDayOfMonth() + "日";
        } catch (Exception e) {
            return date;
        }
    }

    private static String money(double amount) {
        return String.format(Locale.CHINA, "%.2f", amount);
    }
}
