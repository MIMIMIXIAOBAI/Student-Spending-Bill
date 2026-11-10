package com.campusbill.app.chat.ai.assistant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** query_spend：只读，按 schema 的 period / 商户 / 分类筛选。 */
public final class SpendQuery {

    public static final class Result {
        public final double total;
        public final int count;
        public final String periodStart;
        public final String periodEnd;
        public final String json;

        Result(double total, int count, String periodStart, String periodEnd, String json) {
            this.total = total;
            this.count = count;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
            this.json = json;
        }

        public String summary() {
            return String.format(Locale.US, "查询结果：¥%.2f，共 %d 笔（%s 至 %s）。",
                    total, count, periodStart, periodEnd);
        }
    }

    private SpendQuery() {
    }

    public static Result run(List<LedgerBill> bills, JSONObject args, LocalDate today) {
        try {
            return runChecked(bills, args, today);
        } catch (JSONException e) {
            return new Result(0, 0, "", "", "{\"error\":\"json\"}");
        }
    }

    private static Result runChecked(List<LedgerBill> bills, JSONObject args, LocalDate today)
            throws JSONException {
        JSONObject query = args == null ? new JSONObject() : args;
        String period = query.optString("period", "this_week");
        LocalDate start;
        LocalDate end;
        if ("today".equals(period)) {
            start = today;
            end = today;
        } else if ("custom".equals(period)) {
            try {
                start = LocalDate.parse(query.getString("start_date"));
                end = LocalDate.parse(query.getString("end_date"));
            } catch (Exception e) {
                return error("custom 需要 start_date 与 end_date");
            }
        } else {
            WeekRange week = WeekRange.of(today);
            start = week.start;
            end = week.end;
        }

        String type = query.optString("type", "expense");
        String merchantKw = query.optString("merchant_contains", "").trim();
        String categoryKw = query.optString("category", "").trim();
        List<LedgerBill> matched = new ArrayList<>();
        if (bills != null) {
            for (LedgerBill bill : bills) {
                if (bill == null || !inRange(bill.date, start, end) || !typeMatches(bill, type)) {
                    continue;
                }
                if (textMatches(bill, merchantKw, categoryKw)) {
                    matched.add(bill);
                }
            }
        }
        matched.sort(Comparator.comparing((LedgerBill bill) -> bill.date).reversed()
                .thenComparing(bill -> bill.id, Comparator.reverseOrder()));

        long cents = 0;
        for (LedgerBill bill : matched) {
            cents += Math.round(bill.amount * 100.0);
        }
        double total = cents / 100.0;
        JSONObject root = new JSONObject();
        root.put("total", total);
        root.put("count", matched.size());
        root.put("period_start", start.toString());
        root.put("period_end", end.toString());
        root.put("by_category", group(matched, true));
        root.put("by_merchant", group(matched, false));
        JSONArray items = new JSONArray();
        int limit = Math.min(20, matched.size());
        for (int i = 0; i < limit; i++) {
            LedgerBill bill = matched.get(i);
            items.put(new JSONObject()
                    .put("id", bill.id)
                    .put("date", bill.date)
                    .put("amount", bill.amount)
                    .put("category", safe(bill.category))
                    .put("merchant", safe(bill.merchant))
                    .put("remark", safe(bill.remark)));
        }
        root.put("items", items);
        return new Result(total, matched.size(), start.toString(), end.toString(), root.toString());
    }

    private static Result error(String message) throws JSONException {
        String json = new JSONObject().put("error", message).put("total", 0).put("count", 0).toString();
        return new Result(0, 0, "", "", json);
    }

    private static boolean inRange(String date, LocalDate start, LocalDate end) {
        try {
            LocalDate value = LocalDate.parse(date);
            return !value.isBefore(start) && !value.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean typeMatches(LedgerBill bill, String type) {
        if ("income".equals(type)) {
            return bill.type == 1;
        }
        if ("all".equals(type)) {
            return true;
        }
        return bill.type == 0;
    }

    /** 两个筛选词相同（例如都是「食堂」）时按或匹配，避免只命中商户或只命中分类。 */
    private static boolean textMatches(LedgerBill bill, String merchantKw, String categoryKw) {
        boolean merchantHit = merchantKw.isEmpty() || containsBill(bill, merchantKw);
        boolean categoryHit = categoryKw.isEmpty() || contains(bill.category, categoryKw);
        if (!merchantKw.isEmpty() && merchantKw.equals(categoryKw)) {
            return merchantHit || categoryHit;
        }
        return merchantHit && categoryHit;
    }

    private static boolean containsBill(LedgerBill bill, String keyword) {
        return contains(bill.merchant, keyword) || contains(bill.category, keyword)
                || contains(bill.remark, keyword);
    }

    private static boolean contains(String value, String keyword) {
        return value != null && keyword != null && !keyword.isEmpty() && value.contains(keyword);
    }

    private static JSONArray group(List<LedgerBill> bills, boolean byCategory) throws JSONException {
        List<String> keys = new ArrayList<>();
        List<Long> cents = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (LedgerBill bill : bills) {
            String key = safe(byCategory ? bill.category : bill.merchant);
            int index = keys.indexOf(key);
            if (index < 0) {
                keys.add(key);
                cents.add(0L);
                counts.add(0);
                index = keys.size() - 1;
            }
            cents.set(index, cents.get(index) + Math.round(bill.amount * 100.0));
            counts.set(index, counts.get(index) + 1);
        }
        JSONArray array = new JSONArray();
        for (int i = 0; i < keys.size(); i++) {
            JSONObject row = new JSONObject();
            row.put(byCategory ? "category" : "merchant", keys.get(i));
            row.put("total", cents.get(i) / 100.0);
            row.put("count", counts.get(i));
            array.put(row);
        }
        return array;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
