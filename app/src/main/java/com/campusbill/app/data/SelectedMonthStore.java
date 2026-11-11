package com.campusbill.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.YearMonth;

/** 首页图表当前月份。各页读取同一份，避免只靠启动 Intent 里的旧 extra。 */
public final class SelectedMonthStore {

    private static final String PREF = "home_selected_month";
    private static final String KEY = "month";

    private SelectedMonthStore() {
    }

    public static void save(Context context, YearMonth month) {
        if (context == null || month == null) {
            return;
        }
        prefs(context).edit().putString(KEY, month.toString()).apply();
    }

    public static YearMonth get(Context context) {
        if (context == null) {
            return YearMonth.now();
        }
        String value = prefs(context).getString(KEY, "");
        if (value == null || value.trim().isEmpty()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }
}
