package com.campusbill.app.chat.ai.assistant;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模型没发出工具调用时，按 intent-control.v0.1.md 的优先级补一次调用。
 * 工具名和参数形状跟 tools.schema.v0.1.json 一致。
 */
public final class AssistantIntentPlanner {

    private static final Pattern AMOUNT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*元");

    private AssistantIntentPlanner() {
    }

    private static final Pattern NAMED_YEAR_MONTH =
            Pattern.compile("(\\d{4})[-/.年](\\d{1,2})");
    private static final Pattern NAMED_MONTH =
            Pattern.compile("(?<!\\d)(\\d{1,2})月");

    public static List<ToolCall> plan(String text, LocalDate today) {
        return plan(text, today, YearMonth.from(today));
    }

    public static List<ToolCall> plan(String text, LocalDate today, YearMonth chartMonth) {
        try {
            return planChecked(text, today, chartMonth == null ? YearMonth.from(today) : chartMonth);
        } catch (JSONException e) {
            return new ArrayList<>();
        }
    }

    private static List<ToolCall> planChecked(String text, LocalDate today, YearMonth chartMonth)
            throws JSONException {
        List<ToolCall> calls = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return calls;
        }
        String input = text.trim();
        boolean query = isQuery(input);
        boolean bill = isBill(input);
        if (query) {
            calls.add(new ToolCall("call_plan_query", "query_spend",
                    queryArgs(input, today, chartMonth)));
        }
        if (bill) {
            calls.add(new ToolCall("call_plan_bill", "propose_add_bill", billArgs(input, today)));
        }
        if (!query && !bill && input.contains("日记")) {
            String purpose = input.contains("打招呼") || input.contains("早安")
                    ? "greeting" : "tone_adjust";
            calls.add(new ToolCall("call_plan_diary", "request_diary_context",
                    new JSONObject().put("purpose", purpose).toString()));
        }
        if (!query && !bill && isFortune(input)) {
            calls.add(new ToolCall("call_plan_fortune", "daily_fortune",
                    new JSONObject().put("style", fortuneStyle(input)).toString()));
        }
        return calls;
    }

    private static boolean isQuery(String text) {
        if (text.contains("本月总支出") || text.contains("预算")) {
            return false;
        }
        boolean week = text.contains("这周") || text.contains("本周")
                || text.contains("这个星期") || text.contains("本星期");
        boolean today = text.contains("今天") || text.contains("今日");
        boolean canteen = text.contains("食堂");
        boolean monthWords = isSelectedMonthWords(text) || namedMonth(text) != null;
        boolean spend = text.contains("花") || text.contains("支出")
                || text.contains("消费") || text.contains("多少");
        return (week && spend) || (today && spend && !text.contains("运势") && !text.contains("塔罗")
                && !text.contains("晴雨")) || (canteen && spend) || (monthWords && spend);
    }

    private static boolean isSelectedMonthWords(String text) {
        return text.contains("当月") || text.contains("本月") || text.contains("这个月");
    }

    static YearMonth namedMonth(String text, YearMonth chartMonth, LocalDate today) {
        Matcher full = NAMED_YEAR_MONTH.matcher(text);
        if (full.find()) {
            int year = Integer.parseInt(full.group(1));
            int month = Integer.parseInt(full.group(2));
            if (month >= 1 && month <= 12) {
                return YearMonth.of(year, month);
            }
        }
        Matcher bare = NAMED_MONTH.matcher(text);
        if (bare.find()) {
            int month = Integer.parseInt(bare.group(1));
            if (month >= 1 && month <= 12) {
                int year = chartMonth != null ? chartMonth.getYear() : today.getYear();
                return YearMonth.of(year, month);
            }
        }
        return null;
    }

    private static YearMonth namedMonth(String text) {
        return namedMonth(text, null, LocalDate.now());
    }

    private static boolean isBill(String text) {
        if (text.contains("记一笔") || text.contains("记一下") || text.contains("帮我记")
                || text.contains("记上")) {
            return true;
        }
        return AMOUNT.matcher(text).find()
                && (text.contains("午餐") || text.contains("早餐") || text.contains("晚餐")
                || text.contains("食堂") || text.contains("餐饮"));
    }

    private static boolean isFortune(String text) {
        return text.contains("运势") || text.contains("晴雨") || text.contains("塔罗")
                || text.contains("占卜");
    }

    private static String fortuneStyle(String text) {
        if (text.contains("塔罗")) {
            return "tarot_lite";
        }
        if (text.contains("晴雨") || text.contains("情绪")) {
            return "mood";
        }
        return "fortune";
    }

    private static String queryArgs(String text, LocalDate today, YearMonth chartMonth)
            throws JSONException {
        JSONObject args = new JSONObject();
        boolean week = text.contains("这周") || text.contains("本周")
                || text.contains("这个星期") || text.contains("本星期");
        boolean todayWord = text.contains("今天") || text.contains("今日");
        YearMonth named = namedMonth(text, chartMonth, today);
        if (week) {
            args.put("period", "this_week");
        } else if (todayWord && !isSelectedMonthWords(text) && named == null) {
            args.put("period", "today");
        } else if (named != null) {
            args.put("period", "custom");
            args.put("start_date", named.atDay(1).toString());
            args.put("end_date", named.atEndOfMonth().toString());
        } else if (isSelectedMonthWords(text)) {
            args.put("period", "this_month");
        } else {
            args.put("period", "this_week");
        }
        args.put("type", "expense");
        if (text.contains("食堂")) {
            args.put("merchant_contains", "食堂");
            args.put("category", "食堂");
        }
        return args.toString();
    }

    private static String billArgs(String text, LocalDate today) throws JSONException {
        JSONObject args = new JSONObject();
        Matcher matcher = AMOUNT.matcher(text);
        if (matcher.find()) {
            args.put("amount", Double.parseDouble(matcher.group(1)));
        }
        args.put("type", "expense");
        args.put("date", today.toString());
        if (text.contains("食堂")) {
            args.put("category", "食堂");
        } else if (text.contains("午餐") || text.contains("早餐") || text.contains("晚餐")
                || text.contains("餐饮")) {
            args.put("category", "餐饮");
        } else {
            args.put("category", "其他");
        }
        String meal = text.contains("午餐") ? "午餐" : text.contains("早餐") ? "早餐"
                : text.contains("晚餐") ? "晚餐" : "";
        if (!meal.isEmpty()) {
            args.put("merchant", meal);
            args.put("remark", meal);
        }
        return args.toString();
    }
}
