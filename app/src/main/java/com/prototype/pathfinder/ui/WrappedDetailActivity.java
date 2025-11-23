package com.prototype.pathfinder.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.ui.views.RadarChartView;
import com.prototype.pathfinder.utils.RecommendationEngine;

/**
 * Full-screen 6-step "Spotify Wrapped" style storytelling activity.
 * Displays personalized recommendation journey with animated transitions and custom charts.
 * Supports both regular program matches and Bridging intervention path.
 */
public class WrappedDetailActivity extends AppCompatActivity {

    private RecommendationEngine.Recommendation data;
    private int currentStep = 0; // 0 to 5 = 6 total screens

    // UI References
    private TextView tvTitle, tvMainText, tvSubText;
    private ConstraintLayout rootLayout;
    private ProgressBar[] progressBars = new ProgressBar[6];
    private RadarChartView radarChart;
    private FrameLayout chartContainer;  // This is now @+id/chartContainer in XML

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrapped_detail);

        // Get recommendation data passed from Results/Home
        data = (RecommendationEngine.Recommendation) getIntent().getSerializableExtra("rec_data");

        rootLayout = findViewById(R.id.rootLayout);
        tvTitle = findViewById(R.id.tvStoryTitle);
        tvMainText = findViewById(R.id.tvStoryMain);
        tvSubText = findViewById(R.id.tvStorySub);

        // Critical: Use the new chartContainer ID from XML
        chartContainer = findViewById(R.id.chartContainer);
        radarChart = findViewById(R.id.radarChart);

        // Initialize progress indicators
        progressBars[0] = findViewById(R.id.progress1);
        progressBars[1] = findViewById(R.id.progress2);
        progressBars[2] = findViewById(R.id.progress3);
        progressBars[3] = findViewById(R.id.progress4);
        progressBars[4] = findViewById(R.id.progress5);
        progressBars[5] = findViewById(R.id.progress6);

        // Tap anywhere to advance through story
        rootLayout.setOnClickListener(v -> {
            if (currentStep < 5) {
                currentStep++;
                updateUI();
            } else {
                finish();
                overridePendingTransition(0, R.anim.slide_out_down);
            }
        });

        updateUI();
    }

    /**
     * Updates UI for current story step.
     * Handles background color, text, and dynamic chart injection.
     */
    private void updateUI() {
        // Clear previous custom view
        chartContainer.removeAllViews();
        tvSubText.setVisibility(View.VISIBLE);
        radarChart.setVisibility(View.GONE);

        // Update progress dots
        for (int i = 0; i < 6; i++) {
            progressBars[i].setProgress(i <= currentStep ? 100 : 0);
        }

        boolean isBridging = data.program.contains("Bridging");

        switch (currentStep) {
            case 0:
                rootLayout.setBackgroundColor(isBridging ? Color.parseColor("#C62828") : Color.parseColor("#1DB954"));
                tvTitle.setText(isBridging ? "INTERVENTION NEEDED" : "YOUR TOP MATCH");
                tvMainText.setText(data.program);
                tvMainText.setTextSize(40);
                tvSubText.setText(data.matchPercent + "% Priority Score");
                break;

            case 1: // Performance Gap + Tiny Bar Chart
                rootLayout.setBackgroundColor(isBridging ? Color.parseColor("#B71C1C") : Color.parseColor("#536DFE"));
                tvTitle.setText("PERFORMANCE GAP");
                tvMainText.setText("You vs. Requirement");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyWhy);
                chartContainer.addView(new TinyBarChart(this, data.radarValues[0], data.targetScore));
                break;

            case 2: // Skill Balance + Radar Chart
                rootLayout.setBackgroundColor(Color.parseColor("#121212"));
                tvTitle.setText("SKILL BALANCE");
                tvMainText.setText("Quant • Verbal • Logic");
                tvMainText.setTextSize(28);
                tvSubText.setVisibility(View.GONE);
                radarChart.setVisibility(View.VISIBLE);
                radarChart.setValues(data.radarValues[0], data.radarValues[1], data.radarValues[2]);
                chartContainer.addView(radarChart);
                break;

            case 3:
                rootLayout.setBackgroundColor(isBridging ? Color.parseColor("#FF6F00") : Color.parseColor("#8E24AA"));
                tvTitle.setText(isBridging ? "IDENTIFIED WEAKNESS" : "KEY STRENGTH");
                tvMainText.setText(isBridging ? "Algebraic Logic" : "Pattern Recognition");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.itemInsight);
                break;

            case 4: // Success Rate + Tiny Circle Chart
                rootLayout.setBackgroundColor(isBridging ? Color.parseColor("#EF6C00") : Color.parseColor("#E91E63"));
                tvTitle.setText("HISTORICAL DATA");
                tvMainText.setText("Success Probability");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyHistory);
                chartContainer.addView(new TinySuccessCircle(this, data.successRate));
                break;

            case 5:
                rootLayout.setBackgroundColor(isBridging ? Color.parseColor("#F57C00") : Color.parseColor("#FF9800"));
                tvTitle.setText("YOUR TRAJECTORY");
                tvMainText.setText(isBridging ? "Path to Graduation" : "Future Careers");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyCareers);
                break;
        }

        // Fade in main text
        tvMainText.setAlpha(0f);
        tvMainText.animate().alpha(1f).setDuration(600).start();
    }

    // Tiny, beautiful bar chart — only takes ~25% of screen height
    private static class TinyBarChart extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int user, target;

        public TinyBarChart(Context ctx, int user, int target) {
            super(ctx);
            this.user = user;
            this.target = target;
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            if (w == 0 || h == 0) return;

            int barW = w / 7;
            int maxH = h * 65 / 100;
            int bottom = h - dp(28);

            float userH = user / 100f * maxH;
            float targetH = target / 100f * maxH;

            // User bar (green if met/exceeded, red if below)
            p.setColor(user >= target ? 0xFF4CAF50 : 0xFFF44336);
            c.drawRoundRect(w/2f - barW - dp(16), bottom - userH, w/2f - dp(16), bottom, dp(10), dp(10), p);

            // Target bar (gray)
            p.setColor(0xFF666666);
            c.drawRoundRect(w/2f + dp(16), bottom - targetH, w/2f + barW + dp(16), bottom, dp(10), dp(10), p);

            // Labels
            p.setColor(Color.WHITE);
            p.setTextSize(dp(13));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(user + "", w/2f - barW/2 - dp(16), bottom - userH - dp(6), p);
            c.drawText(target + "", w/2f + barW/2 + dp(16), bottom - targetH - dp(6), p);
        }

        private int dp(int dp) {
            return (int) (dp * getResources().getDisplayMetrics().density);
        }
    }

    // Tiny success circle — super clean
    private static class TinySuccessCircle extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int percent;
        private final RectF rect = new RectF();

        public TinySuccessCircle(Context ctx, int percent) {
            super(ctx);
            this.percent = Math.min(100, Math.max(0, percent));
        }

        @Override protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.29f;

            // Background ring
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(9));
            p.setColor(0xFF333333);
            c.drawCircle(cx, cy, radius, p);

            // Progress arc
            p.setColor(Color.WHITE);
            p.setStrokeCap(Paint.Cap.ROUND);
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius);
            c.drawArc(rect, -90, 3.6f * percent, false, p);

            // Percentage text
            p.setStyle(Paint.Style.FILL);
            p.setTextSize(dp(34));
            p.setTextAlign(Paint.Align.CENTER);
            c.drawText(percent + "%", cx, cy + dp(11), p);

            // Label
            p.setTextSize(dp(12));
            p.setAlpha(180);
            c.drawText("Success Rate", cx, cy + dp(36), p);
        }

        private int dp(int dp) {
            return (int) (dp * getResources().getDisplayMetrics().density);
        }
    }
}