package com.prototype.pathfinder.ui.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class RadarChartView extends View {

    private int[] values = {70, 70, 70};
    private final Paint webPaint = new Paint();
    private final Paint fillPaint = new Paint();
    private final Paint strokePaint = new Paint();
    private final Paint labelPaint = new Paint();

    // SHORT, BOLD LABELS — NEVER CUT OFF
    private final String[] labels = {"QUANT", "VERBAL", "LOGIC"};

    public RadarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);

        webPaint.setColor(Color.parseColor("#40FFFFFF"));
        webPaint.setStyle(Paint.Style.STROKE);
        webPaint.setStrokeWidth(3f);
        webPaint.setAntiAlias(true);

        fillPaint.setColor(Color.parseColor("#4CAF50"));
        fillPaint.setAlpha(100);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        strokePaint.setColor(Color.parseColor("#4CAF50"));
        strokePaint.setStrokeWidth(16f);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setAntiAlias(true);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextSize(spToPx(16));
        labelPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setAntiAlias(true);
        labelPaint.setShadowLayer(10f, 0, 4, Color.BLACK);
    }

    public void setValues(int quant, int verbal, int logical) {
        values[0] = Math.max(0, Math.min(100, quant));
        values[1] = Math.max(0, Math.min(100, verbal));
        values[2] = Math.max(0, Math.min(100, logical));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.78f;  // Safe radius

        // === Grid ===
        for (int level = 1; level <= 5; level++) {
            Path path = new Path();
            for (int i = 0; i < 3; i++) {
                double angle = Math.toRadians(i * 120 - 90);
                float r = radius * level / 5f;
                float x = cx + r * (float) Math.cos(angle);
                float y = cy + r * (float) Math.sin(angle);
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            path.close();
            canvas.drawPath(path, webPaint);
        }

        // === Data Triangle ===
        Path dataPath = new Path();
        Path strokePath = new Path();
        boolean first = true;

        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(i * 120 - 90);
            float percent = values[i] / 100f;
            float x = cx + radius * percent * (float) Math.cos(angle);
            float y = cy + radius * percent * (float) Math.sin(angle);

            if (first) {
                dataPath.moveTo(x, y);
                strokePath.moveTo(x, y);
                first = false;
            } else {
                dataPath.lineTo(x, y);
                strokePath.lineTo(x, y);
            }
        }
        dataPath.close();
        strokePath.close();
        canvas.drawPath(dataPath, fillPaint);
        canvas.drawPath(strokePath, strokePaint);

        // === LABELS — NOW 100% INSIDE, NEVER CUT OFF ===
        float labelRadius = radius * 0.68f;  // Inside the chart — safe zone

        for (int i = 0; i < 3; i++) {
            double angle = Math.toRadians(i * 120 - 90);
            float x = cx + labelRadius * (float) Math.cos(angle);
            float y = cy + labelRadius * (float) Math.sin(angle);

            // Perfect vertical centering
            Paint.FontMetrics fm = labelPaint.getFontMetrics();
            float baseline = y + (fm.bottom - fm.top) / 2 - fm.bottom;

            canvas.drawText(labels[i], x, baseline, labelPaint);
        }
    }

    private float spToPx(float sp) {
        return sp * getContext().getResources().getDisplayMetrics().scaledDensity;
    }
}