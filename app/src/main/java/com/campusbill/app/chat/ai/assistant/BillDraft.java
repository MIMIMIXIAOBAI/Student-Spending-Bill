package com.campusbill.app.chat.ai.assistant;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Locale;

/** propose_add_bill 的预览。构造它不会写账。 */
public final class BillDraft {
    public final int type;
    public final double amount;
    public final String category;
    public final String merchant;
    public final String remark;
    public final String date;
    public final String time;

    private BillDraft(int type, double amount, String category, String merchant,
                      String remark, String date, String time) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.merchant = merchant;
        this.remark = remark;
        this.date = date;
        this.time = time;
    }

    public static BillDraft parse(String arguments, LocalDate today) {
        JSONObject args;
        try {
            args = arguments == null || arguments.trim().isEmpty()
                    ? new JSONObject() : new JSONObject(arguments);
        } catch (Exception e) {
            args = new JSONObject();
        }
        double amount = args.optDouble("amount", 0);
        String category = args.optString("category", "").trim();
        if (category.isEmpty()) {
            category = "其他";
        }
        String date = args.optString("date", "").trim();
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            date = today.toString();
        }
        String time = args.optString("time", "").trim();
        if (time.matches("\\d{2}:\\d{2}")) {
            time = time + ":00";
        } else if (!time.matches("\\d{2}:\\d{2}:\\d{2}")) {
            time = "12:00:00";
        }
        int type = "income".equals(args.optString("type", "expense")) ? 1 : 0;
        return new BillDraft(type, amount, category,
                args.optString("merchant", "").trim(),
                args.optString("remark", "").trim(),
                date, time);
    }

    public boolean valid() {
        return amount >= 0.01 && date != null && date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public LedgerBill toBill() {
        LedgerBill bill = new LedgerBill();
        bill.type = type;
        bill.amount = amount;
        bill.category = category;
        bill.merchant = merchant;
        bill.remark = remark;
        bill.date = date;
        bill.time = time;
        return bill;
    }

    public JSONObject preview() {
        return new JSONObject()
                .put("type", type == 1 ? "income" : "expense")
                .put("amount", amount)
                .put("category", category)
                .put("merchant", merchant)
                .put("remark", remark)
                .put("date", date)
                .put("time", time);
    }

    public String cardBody() {
        return (type == 1 ? "收入" : "支出")
                + String.format(Locale.US, " ¥%.2f", amount)
                + "\n分类 " + category
                + "\n商户 " + (merchant.isEmpty() ? "—" : merchant)
                + "\n日期 " + date
                + "\n备注 " + (remark.isEmpty() ? "—" : remark);
    }

    public boolean matches(LedgerBill saved) {
        if (saved == null) {
            return false;
        }
        return saved.type == type
                && Math.round(saved.amount * 100.0) == Math.round(amount * 100.0)
                && category.equals(saved.category)
                && merchant.equals(nullToEmpty(saved.merchant))
                && remark.equals(nullToEmpty(saved.remark))
                && date.equals(saved.date);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
