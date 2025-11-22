package com.prototype.pathfinder.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.ui.views.RadarChartView;
import com.prototype.pathfinder.utils.RecommendationEngine;

/**
 * WrappedDetailActivity
 * <p>
 * Displays a detailed, gamified breakdown of a specific academic recommendation.
 * Inspired by "Spotify Wrapped," it uses a tap-to-advance story format.
 * * Flow Steps:
 * 0. Top Match Reveal
 * 1. "Why This Fits" (Text explanation)
 * 2. Aptitude Breakdown (Radar Chart visualization)
 * 3. Specific Insight (e.g., "You crushed Question #17")
 * 4. Historical Data Success
 * 5. Future Career Outlook
 */
public class WrappedDetailActivity extends AppCompatActivity {

    private RecommendationEngine.Recommendation data;
    private int currentStep = 0; // Tracks the current story slide (0-5)

    // UI Components
    private TextView tvTitle, tvMainText, tvSubText;
    private ConstraintLayout rootLayout;
    private ProgressBar[] progressBars = new ProgressBar[6]; // Top progress indicators
    private RadarChartView radarChart; // Custom view for skill visualization

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wrapped_detail);

        // Retrieve the Data Object
        data = (RecommendationEngine.Recommendation) getIntent().getSerializableExtra("rec_data");

        // Bind Views
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

        // Tap Listener for Navigation
        rootLayout.setOnClickListener(v -> {
            if (currentStep < 5) {
                currentStep++;
                updateUI();
            } else {
                // End of story, close activity with slide-down animation
                finish();
                overridePendingTransition(0, R.anim.slide_out_down);
            }
        });

        // Initial Load
        updateUI();
    }

    /**
     * Updates the UI elements based on the `currentStep`.
     * Changes background colors, text content, and visibility of complex views (RadarChart).
     */
    private void updateUI() {
        // Reset specific views
        radarChart.setVisibility(View.GONE);
        tvSubText.setVisibility(View.VISIBLE);

        // Update Top Progress Bars (Segments)
        for (int i = 0; i < 6; i++) {
            progressBars[i].setProgress(i <= currentStep ? 100 : 0);
        }

        // State Machine for Story Content
        switch (currentStep) {
            case 0: // REVEAL
                rootLayout.setBackgroundColor(Color.parseColor("#1DB954")); // Brand Green
                tvTitle.setText("YOUR TOP MATCH");
                tvMainText.setText(data.program);
                tvMainText.setTextSize(48);
                tvSubText.setText(data.matchPercent + "% Match");
                break;

            case 1: // WHY IT FITS
                rootLayout.setBackgroundColor(Color.parseColor("#536DFE")); // Indigo
                tvTitle.setText("WHY THIS FITS");
                tvMainText.setText("Your Aptitude Profile");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyWhy);
                break;

            case 2: // RADAR CHART (VISUALIZATION)
                rootLayout.setBackgroundColor(Color.parseColor("#121212")); // Dark Mode
                tvTitle.setText("YOUR SCORE BREAKDOWN");
                tvMainText.setText("Quantitative • Verbal • Logical");
                tvMainText.setTextSize(28);
                tvSubText.setVisibility(View.GONE); // Hide subtext to make room for chart
                radarChart.setVisibility(View.VISIBLE);
                // Set data for custom drawing
                radarChart.setValues(data.radarValues[0], data.radarValues[1], data.radarValues[2]);
                break;

            case 3: // INSIGHT / HARDEST QUESTION
                rootLayout.setBackgroundColor(Color.parseColor("#8E24AA")); // Purple
                tvTitle.setText("YOU CRUSHED THE HARDEST ONE");
                tvMainText.setText("Legendary Question #17");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.itemInsight);
                break;

            case 4: // HISTORICAL DATA
                rootLayout.setBackgroundColor(Color.parseColor("#E91E63")); // Pink
                tvTitle.setText("THE DATA SAYS");
                tvMainText.setText("Historical Success");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyHistory);
                break;

            case 5: // CAREERS
                rootLayout.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                tvTitle.setText("YOUR FUTURE");
                tvMainText.setText("Careers Await");
                tvMainText.setTextSize(32);
                tvSubText.setText(data.storyCareers);
                break;
        }

        // Apply Text Fade-in Animation for smooth transitions
        tvMainText.setAlpha(0f);
        tvMainText.animate().alpha(1f).setDuration(500).start();
        if (tvSubText.getVisibility() == View.VISIBLE) {
            tvSubText.setAlpha(0f);
            tvSubText.animate().alpha(1f).setDuration(600).start();
        }
    }
}