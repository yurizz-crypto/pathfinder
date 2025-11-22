package com.prototype.pathfinder.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.utils.RecommendationEngine;

public class WrappedDetailActivity extends AppCompatActivity {

    private RecommendationEngine.Recommendation data;
    private int currentStep = 0; // 0: Intro, 1: Why, 2: History, 3: Career
    private TextView tvTitle, tvMainText, tvSubText;
    private ConstraintLayout rootLayout;
    private ProgressBar[] progressBars = new ProgressBar[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrapped_detail);

        data = (RecommendationEngine.Recommendation) getIntent().getSerializableExtra("rec_data");

        rootLayout = findViewById(R.id.rootLayout);
        tvTitle = findViewById(R.id.tvStoryTitle);
        tvMainText = findViewById(R.id.tvStoryMain);
        tvSubText = findViewById(R.id.tvStorySub);

        // Bind Progress Bars
        progressBars[0] = findViewById(R.id.progress1);
        progressBars[1] = findViewById(R.id.progress2);
        progressBars[2] = findViewById(R.id.progress3);
        progressBars[3] = findViewById(R.id.progress4);

        // Tap anywhere to advance
        rootLayout.setOnClickListener(v -> {
            if (currentStep < 3) {
                currentStep++;
                updateUI();
            } else {
                finish(); // Close story on last step
                overridePendingTransition(0, R.anim.slide_out_down);
            }
        });

        updateUI();
    }

    private void updateUI() {
        // Update Progress Bars
        for (int i = 0; i < 4; i++) {
            progressBars[i].setProgress(i <= currentStep ? 100 : 0);
        }

        // Update Content based on Step
        switch (currentStep) {
            case 0: // The Reveal
                rootLayout.setBackgroundColor(Color.parseColor("#1DB954")); // Spotify Green-ish
                tvTitle.setText("TOP MATCH");
                tvMainText.setText(data.program);
                tvMainText.setTextSize(48);
                tvSubText.setText(data.matchPercent + "% Match");
                break;

            case 1: // The Aptitude (Why)
                rootLayout.setBackgroundColor(Color.parseColor("#536DFE")); // Indigo
                tvTitle.setText("YOUR APTITUDE");
                tvMainText.setText("Why this fits...");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyWhy);
                break;

            case 2: // The History (Stats)
                rootLayout.setBackgroundColor(Color.parseColor("#E91E63")); // Pink
                tvTitle.setText("SUCCESS METRICS");
                tvMainText.setText("The Data says...");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyHistory);
                break;

            case 3: // The Future (Jobs)
                rootLayout.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                tvTitle.setText("YOUR FUTURE");
                tvMainText.setText("Potential Careers");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyCareers);
                break;
        }

        // Simple fade animation for text change
        tvMainText.setAlpha(0f);
        tvMainText.animate().alpha(1f).setDuration(500).start();
    }
}