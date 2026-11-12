package com.campusbill.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.campusbill.app.AiChatActivity;
import com.campusbill.app.CalendarActivity;
import com.campusbill.app.HistoryActivity;
import com.campusbill.app.MainActivity;
import com.campusbill.app.R;
import com.campusbill.app.data.SelectedMonthStore;

public final class BottomTabHelper {
    private static final String EXTRA_MONTH = "extra_month";

    public enum Tab {
        HOME(R.id.tabHome, MainActivity.class),
        CALENDAR(R.id.tabCalendar, CalendarActivity.class),
        AI(R.id.tabAi, AiChatActivity.class),
        HISTORY(R.id.tabHistory, HistoryActivity.class);

        final int viewId;
        final Class<?> destination;

        Tab(int viewId, Class<?> destination) {
            this.viewId = viewId;
            this.destination = destination;
        }
    }

    private BottomTabHelper() {}

    public static void bind(Activity activity, Tab selected) {
        int selectedColor = ContextCompat.getColor(activity, R.color.color_sky);
        int normalColor = ContextCompat.getColor(activity, R.color.text_secondary);
        for (Tab tab : Tab.values()) {
            View item = activity.findViewById(tab.viewId);
            if (item == null) {
                continue;
            }
            boolean isSelected = tab == selected;
            applyState(item, isSelected, selectedColor, normalColor);
            item.setOnClickListener(v -> open(activity, tab, selected));
        }
        View bar = activity.findViewById(R.id.bottomTabs);
        if (bar != null) {
            applyInsets(bar);
        }
    }

    private static void open(Activity activity, Tab target, Tab current) {
        if (target == current || activity.getClass() == target.destination) {
            return;
        }
        Intent intent = new Intent(activity, target.destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (target == Tab.CALENDAR || target == Tab.AI) {
            intent.putExtra(EXTRA_MONTH, SelectedMonthStore.get(activity).toString());
        }
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.tab_fade_in, R.anim.tab_fade_out);
    }

    private static void applyState(View item, boolean selected, int selectedColor, int normalColor) {
        int color = selected ? selectedColor : normalColor;
        item.setBackgroundResource(selected ? R.drawable.bg_tab_item_selected : 0);
        if (item instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) item;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) {
                    TextView label = (TextView) child;
                    label.setTextColor(color);
                    label.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
    }

    private static void applyInsets(View bar) {
        final int padLeft = bar.getPaddingLeft();
        final int padTop = bar.getPaddingTop();
        final int padRight = bar.getPaddingRight();
        final int padBottom = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, insets) -> {
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            v.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            v.setPadding(padLeft, padTop, padRight, padBottom + (imeVisible ? 0 : navBottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(bar);
    }
}
