package com.prototype.pathfinder.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.ui.views.RadarChartView; // Make sure this matches your package
import com.prototype.pathfinder.utils.RecommendationEngine;

public class WrappedDetailActivity extends AppCompatActivity {

    private RecommendationEngine.Recommendation data;
    private int currentStep = 0;
    private TextView tvTitle, tvMainText, tvSubText;
    private ConstraintLayout rootLayout;
    private ProgressBar[] progressBars = new ProgressBar[6];
    private RadarChartView radarChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrapped_detail);

        data = (RecommendationEngine.Recommendation) getIntent().getSerializableExtra("rec_data");

        rootLayout = findViewById(R.id.rootLayout);
        tvTitle = findViewById(R.id.tvStoryTitle);
        tvMainText = findViewById(R.id.tvStoryMain);
        tvSubText = findViewById(R.id.tvStorySub);
        radarChart = findViewById(R.id.radarChart);

        progressBars[0] = findViewById(R.id.progress1);
        progressBars[1] = findViewById(R.id.progress2);
        progressBars[2] = findViewById(R.id.progress3);
        progressBars[3] = findViewById(R.id.progress4);
        progressBars[4] = findViewById(R.id.progress5);
        progressBars[5] = findViewById(R.id.progress6);

        // FIXED: was "dosis" → now correct
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

    private void updateUI() {
        radarChart.setVisibility(View.GONE);
        tvSubText.setVisibility(View.VISIBLE);

        for (int i = 0; i < 6; i++) {
            progressBars[i].setProgress(i <= currentStep ? 100 : 0);
        }

        switch (currentStep) {
            case 0: // Reveal
                rootLayout.setBackgroundColor(Color.parseColor("#1DB954"));
                tvTitle.setText("YOUR TOP MATCH");
                tvMainText.setText(data.program);
                tvMainText.setTextSize(48);
                tvSubText.setText(data.matchPercent + "% Match");
                break;

            case 1: // Why this program fits
                rootLayout.setBackgroundColor(Color.parseColor("#536DFE"));
                tvTitle.setText("WHY THIS FITS");
                tvMainText.setText("Your Aptitude Profile");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyWhy);
                break;

            case 2: // Radar Chart
                rootLayout.setBackgroundColor(Color.parseColor("#121212"));
                tvTitle.setText("YOUR SCORE BREAKDOWN");
                tvMainText.setText("Quantitative • Verbal • Logical");
                tvMainText.setTextSize(28);
                tvSubText.setVisibility(View.GONE);
                radarChart.setVisibility(View.VISIBLE);
                radarChart.setValues(data.radarValues[0], data.radarValues[1], data.radarValues[2]);
                break;

            case 3: // Legendary Question #17
                rootLayout.setBackgroundColor(Color.parseColor("#8E24AA"));
                tvTitle.setText("YOU CRUSHED THE HARDEST ONE");
                tvMainText.setText("Legendary Question #17");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.itemInsight);
                break;

            case 4: // Success Metrics
                rootLayout.setBackgroundColor(Color.parseColor("#E91E63"));
                tvTitle.setText("THE DATA SAYS");
                tvMainText.setText("Historical Success");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyHistory);
                break;

            case 5: // Future Careers
                rootLayout.setBackgroundColor(Color.parseColor("#FF9800"));
                tvTitle.setText("YOUR FUTURE");
                tvMainText.setText("Careers Await");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyCareers);
                break;
        }

        // Fade-in animation
        tvMainText.setAlpha(0f);
        tvMainText.animate().alpha(1f).setDuration(500).start();
        if (tvSubText.getVisibility() == View.VISIBLE) {
            tvSubText.setAlpha(0f);
            tvSubText.animate().alpha(1f).setDuration(600).start();
        }
    }
}