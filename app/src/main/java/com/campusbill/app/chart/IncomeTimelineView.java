package com.campusbill.app.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.campusbill.app.R;
import com.campusbill.app.data.BillEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 收入时间轴：从下往上绘制，最下方为当月最早日期；
 * 每个有收入的日期一个节点，横线长度与金额成正比，节点与横线使用该分类颜色。
 * 交互采用新拟态风格：按下时记录行「凹下」，选中后「凸起」表达当前状态。
 */
public class IncomeTimelineView extends View {

    public interface OnNodeClickListener {
        void onNodeClick(int index, BillEntity bill, int color);
    }

    private final List<BillEntity> bills = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private final List<Float> centers = new ArrayList<>();
    // 预计算的每行标签与最大金额，避免 onDraw 中反复 String.format 与遍历
    private final List<String> labels = new ArrayList<>();
    private float maxAmount = 1f;

    private Paint spinePaint;
    private Paint barPaint;
    private Paint nodePaint;
    private Paint textPaint;
    private Paint emptyPaint;
    private Paint shadowPaint;
    private Paint raisedPaint;
    private Paint insetPaint;
    private Paint insetLinePaint;
    private Paint highlightLinePaint;
    private Paint ringPaint;
    private final RectF barRect = new RectF();
    private final RectF rowRect = new RectF();
    private final RectF shadowRect = new RectF();
    private final RectF innerRect = new RectF();

    private float density;
    private float rowHeight;
    private float paddingVertical;
    private float nodeRadius;
    private float barHeight;
    private float barStart;
    private float paddingHorizontal;
    private float touchSlop;

    private OnNodeClickListener listener;
    private String emptyText;
    private int pressedIndex = -1;
    private int selectedIndex = -1;
    private float downY;

    public IncomeTimelineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density = getResources().getDisplayMetrics().density;
        float scaled = getResources().getDisplayMetrics().scaledDensity;
        rowHeight = 62f * density;
        paddingVertical = 18f * density;
        nodeRadius = 5f * density;
        barHeight = 8f * density;
        paddingHorizontal = 4f * density;
        touchSlop = 6f * density;

        spinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        spinePaint.setColor(0x33C6B2FF);
        spinePaint.setStyle(Paint.Style.STROKE);
        spinePaint.setStrokeWidth(2f * density);
        spinePaint.setPathEffect(new DashPathEffect(
                new float[]{6f * density, 6f * density}, 0f));

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF1F2430);
        textPaint.setTextSize(12f * scaled);

        emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptyPaint.setColor(0xFF9AA3AF);
        emptyPaint.setTextSize(13f * scaled);
        emptyPaint.setTextAlign(Paint.Align.CENTER);
        emptyText = context.getString(R.string.income_empty);

        // 新拟态：凸起用白色卡面 + 右下柔和投影，凹下用浅灰蓝底 + 上暗下亮的浮雕线
        shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x33203A5C);
        raisedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        raisedPaint.setColor(0xFFFFFFFF);
        insetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        insetPaint.setColor(0xFFE2EDFA);
        insetLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        insetLinePaint.setColor(0x3389A6C9);
        insetLinePaint.setStyle(Paint.Style.STROKE);
        insetLinePaint.setStrokeWidth(1.2f * density);
        highlightLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightLinePaint.setColor(0xCCFFFFFF);
        highlightLinePaint.setStyle(Paint.Style.STROKE);
        highlightLinePaint.setStrokeWidth(1.2f * density);

        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(0xCCFFFFFF);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(1.4f * density);
    }

    /** 无数据时的提示文案（收入 / 消费页面分别设置） */
    public void setEmptyText(String text) {
        this.emptyText = text;
        invalidate();
    }

    /** 设置选中行（-1 表示无选中），选中行以凸起效果标识 */
    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        invalidate();
    }

    /** 设置数据：bills 按时间升序，colorMap 为分类颜色映射 */
    public void setData(List<BillEntity> data, Map<String, Integer> colorMap) {
        bills.clear();
        colors.clear();
        centers.clear();
        labels.clear();
        maxAmount = 1f;
        pressedIndex = -1;
        selectedIndex = -1;
        if (data != null) {
            bills.addAll(data);
            float max = 0f;
            for (BillEntity bill : bills) {
                Integer color = colorMap == null ? null : colorMap.get(bill.category);
                colors.add(color == null ? 0xFF9FD4FF : color);
                labels.add(String.format(Locale.CHINA, "%s · %s  ¥%.2f",
                        shortDate(bill.date), bill.category, bill.amount));
                max = Math.max(max, (float) bill.amount);
            }
            if (max > 0f) {
                maxAmount = max;
            }
        }
        requestLayout();
        invalidate();
    }

    public void setOnNodeClickListener(OnNodeClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        float desired;
        if (bills.isEmpty()) {
            desired = 72f * density;
        } else {
            desired = paddingVertical * 2f + rowHeight * bills.size();
        }
        setMeasuredDimension(width, resolveSize((int) desired, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();

        if (bills.isEmpty()) {
            canvas.drawText(emptyText, width / 2f, height / 2f, emptyPaint);
            return;
        }

        float axisX = paddingHorizontal + nodeRadius * 2.2f;
        float startX = axisX + nodeRadius + 10f * density;
        float maxBarWidth = Math.max(40f, width - paddingHorizontal - startX);

        centers.clear();
        // 从上往下的虚线主轴
        canvas.drawLine(axisX, paddingVertical, axisX, height - paddingVertical, spinePaint);

        for (int i = 0; i < bills.size(); i++) {
            BillEntity bill = bills.get(i);
            int color = colors.get(i);
            // 索引 0 为最早日期，画在最下方；索引增大向上排列
            float centerY = height - paddingVertical - rowHeight * i - rowHeight / 2f;
            centers.add(centerY);

            drawRowState(canvas, width, centerY, i);

            float length = Math.max(28f, maxBarWidth * ((float) bill.amount / maxAmount));
            barRect.set(startX, centerY - barHeight / 2f,
                    startX + length, centerY + barHeight / 2f);
            barPaint.setColor(color);
            canvas.drawRoundRect(barRect, barHeight / 2f, barHeight / 2f, barPaint);

            nodePaint.setColor(color);
            float radius = selectedIndex == i ? nodeRadius + 1.5f * density : nodeRadius;
            canvas.drawCircle(axisX, centerY, radius, nodePaint);
            if (selectedIndex == i) {
                canvas.drawCircle(axisX, centerY, radius + 1.2f * density, ringPaint);
            }

            canvas.drawText(labels.get(i), startX,
                    centerY - barHeight / 2f - 5f * density, textPaint);
        }
    }

    /** 绘制行的新拟态状态：选中凸起，按下凹下，其余保持通透 */
    private void drawRowState(Canvas canvas, float width, float centerY, int index) {
        if (index != selectedIndex && index != pressedIndex) {
            return;
        }
        float radius = 16f * density;
        rowRect.set(paddingHorizontal - 2f * density,
                centerY - rowHeight / 2f + 5f * density,
                width - paddingHorizontal + 2f * density,
                centerY + rowHeight / 2f - 5f * density);

        if (index == selectedIndex) {
            // 凸起：右下投影 + 白色卡面
            shadowRect.set(rowRect);
            shadowRect.offset(3.2f * density, 3.2f * density);
            canvas.drawRoundRect(shadowRect, radius, radius, shadowPaint);
            canvas.drawRoundRect(rowRect, radius, radius, raisedPaint);
            canvas.drawRoundRect(rowRect, radius, radius, highlightLinePaint);
        } else {
            // 凹下：浅底 + 上暗下亮的内阴影描边
            canvas.drawRoundRect(rowRect, radius, radius, insetPaint);
            innerRect.set(rowRect);
            innerRect.inset(1.2f * density, 1.2f * density);
            canvas.drawRoundRect(innerRect, radius - 1.2f * density, radius - 1.2f * density,
                    insetLinePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bills.isEmpty() || listener == null) {
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                pressedIndex = findIndexByY(downY);
                if (pressedIndex >= 0) {
                    invalidate();
                }
                return pressedIndex >= 0;
            case MotionEvent.ACTION_MOVE:
                if (pressedIndex >= 0
                        && Math.abs(event.getY() - downY) > touchSlop * 2f) {
                    pressedIndex = -1;
                    invalidate();
                }
                return pressedIndex >= 0;
            case MotionEvent.ACTION_UP:
                int index = findIndexByY(event.getY());
                boolean pressed = pressedIndex >= 0;
                pressedIndex = -1;
                if (pressed && index >= 0) {
                    performClick();
                    selectedIndex = index;
                    invalidate();
                    listener.onNodeClick(index, bills.get(index), colors.get(index));
                    return true;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (pressedIndex >= 0) {
                    pressedIndex = -1;
                    invalidate();
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /** 按落点纵坐标定位节点行 */
    private int findIndexByY(float y) {
        for (int i = 0; i < centers.size(); i++) {
            if (Math.abs(y - centers.get(i)) <= rowHeight / 2f) {
                return i;
            }
        }
        return -1;
    }

    /** yyyy-MM-dd 转为 MM-dd */
    private String shortDate(String date) {
        if (date != null && date.length() >= 10) {
            return date.substring(5);
        }
        return date == null ? "" : date;
    }
}
