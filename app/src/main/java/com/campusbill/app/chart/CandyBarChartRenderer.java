package com.campusbill.app.chart;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

/**
 * 糖果风柱状图渲染器：柱体圆角，填充自底部白色向上渐变到该分类的糖果色。
 */
public class CandyBarChartRenderer extends BarChartRenderer {

    private static final int BOTTOM_COLOR = 0xFFFFFFFF;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect = new RectF();
    private final float cornerRadius;

    public CandyBarChartRenderer(BarChart chart, ChartAnimator animator,
                                 ViewPortHandler viewPortHandler) {
        super(chart, animator, viewPortHandler);
        this.cornerRadius = Utils.convertDpToPixel(10f);
    }

    @Override
    protected void drawDataSet(Canvas c, IBarDataSet dataSet, int index) {
        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());
        float phaseY = mAnimator.getPhaseY();
        float barWidth = mChart.getBarData().getBarWidth();

        c.save();
        c.clipRect(mViewPortHandler.getContentRect());

        for (int i = 0; i < dataSet.getEntryCount(); i++) {
            BarEntry entry = dataSet.getEntryForIndex(i);
            if (entry == null) {
                continue;
            }

            barRect.set(entry.getX() - barWidth / 2f, 0f,
                    entry.getX() + barWidth / 2f, entry.getY());
            trans.rectToPixelPhase(barRect, phaseY);

            float top = Math.min(barRect.top, barRect.bottom);
            float bottom = Math.max(barRect.top, barRect.bottom);
            float height = bottom - top;
            float width = barRect.width();
            if (height <= 0f || width <= 0f) {
                continue;
            }

            barRect.set(barRect.left, top, barRect.right, bottom);
            float radius = Math.min(cornerRadius, Math.min(width / 2f, height / 2f));

            int color = dataSet.getColor(i);
            barPaint.setShader(new LinearGradient(0f, top, 0f, bottom,
                    new int[]{color, BOTTOM_COLOR}, null, Shader.TileMode.CLAMP));
            c.drawRoundRect(barRect, radius, radius, barPaint);
        }

        c.restore();
        barPaint.setShader(null);
    }
}
