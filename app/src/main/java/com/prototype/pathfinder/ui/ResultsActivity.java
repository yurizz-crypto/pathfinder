package com.prototype.pathfinder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;
import com.prototype.pathfinder.utils.RecommendationEngine;
import java.util.List;
import java.util.Map;

/**
 * ResultsActivity
 * <p>
 * Calculates and displays the academic program recommendations based on user input.
 * Workflow:
 * 1. Receives Test ID and Survey Data from the previous activity.
 * 2. Simulates a calculation delay (with Lottie animation).
 * 3. Invokes RecommendationEngine to process scores.
 * 4. Saves the results locally for the Home Dashboard.
 * 5. Displays the top recommendations in a list.
 */
public class ResultsActivity extends AppCompatActivity {
    // UI Components
    private LottieAnimationView lottieReveal;
    private ProgressBar pbFallback;
    private RecyclerView rvRecs;
    private TextView tvConfidence;
    private TextView tvCalculating;
    private NestedScrollView nestedResults;

    // Logic Components
    private DBManager dbManager;
    private RecommendationEngine engine;
    private List<RecommendationEngine.Recommendation> recs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Init UI
        lottieReveal = findViewById(R.id.lottieReveal);
        pbFallback = findViewById(R.id.pbRevealFallback);
        tvCalculating = findViewById(R.id.tvCalculating);
        rvRecs = findViewById(R.id.rvRecommendations);
        tvConfidence = findViewById(R.id.tvConfidence);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnHome = findViewById(R.id.btnHome);
        nestedResults = findViewById(R.id.nestedResults);

        // Init Database & Engine
        dbManager = new DBManager(this);
        dbManager.open();
        engine = new RecommendationEngine(dbManager);

        // Retrieve Data passed via Intent
        String testId = getIntent().getStringExtra("test_id");
        Bundle bundle = getIntent().getBundleExtra("bundle");
        Map<String, Integer> survey = (Map<String, Integer>) bundle.getSerializable("survey_scores");

        // --- CALCULATION & ANIMATION DELAY ---
        new Handler().postDelayed(() -> {
            // 1. Hide Loading UI / Show Results UI
            lottieReveal.setVisibility(View.GONE);
            pbFallback.setVisibility(View.GONE);
            tvCalculating.setVisibility(View.GONE);
            nestedResults.setVisibility(View.VISIBLE);

            // 2. Perform Calculation
            recs = engine.computeRecommendations(testId, survey);

            // 3. Save results to SharedPreferences (for Home Fragment persistence)
            saveResultsToPrefs(recs);

            // 4. Setup RecyclerView
            rvRecs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rvRecs.setAdapter(new RecAdapter(recs));

        }, 2000); // 2 second delay for "Reveal" effect

        // Listener for Share Button (Placeholder)
        btnShare.setOnClickListener(v -> { /* Share Intent Logic */ });

        // Listener for Home Button - Clears stack and returns to Dashboard
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Persists the Top 3 recommendations to SharedPreferences.
     * This allows the HomeFragment to display the results later without re-calculating.
     *
     * @param list The list of calculated recommendations.
     */
    private void saveResultsToPrefs(List<RecommendationEngine.Recommendation> list) {
        SharedPreferences prefs = getSharedPreferences("user_results", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int count = Math.min(list.size(), 3); // Store max 3
        editor.putInt("rec_count", count);

        for (int i = 0; i < count; i++) {
            RecommendationEngine.Recommendation r = list.get(i);
            // Save Text Data
            editor.putString("rec_prog_" + i, r.program);
            editor.putInt("rec_pct_" + i, r.matchPercent);
            editor.putString("rec_why_" + i, r.storyWhy);
            editor.putString("rec_hist_" + i, r.storyHistory);
            editor.putString("rec_car_" + i, r.storyCareers);

            // Save Analysis Data (for Wrapped View)
            editor.putString("rec_insight_" + i, r.itemInsight);
            editor.putString("rec_hardest_" + i, r.hardestLogical);

            // Save Raw Radar Scores
            editor.putInt("rec_quant_" + i, r.radarValues[0]);
            editor.putInt("rec_verbal_" + i, r.radarValues[1]);
            editor.putInt("rec_logical_" + i, r.radarValues[2]);
        }
        editor.apply();
    }

    // --- ADAPTER CLASS ---

    /**
     * RecyclerView Adapter for displaying the result cards.
     */
    private class RecAdapter extends RecyclerView.Adapter<RecAdapter.ViewHolder> {
        private final List<RecommendationEngine.Recommendation> recsList;

        public RecAdapter(List<RecommendationEngine.Recommendation> r) { this.recsList = r; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recommendation, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RecommendationEngine.Recommendation rec = recsList.get(position);
            holder.tvProgram.setText(rec.program);
            holder.pbMatch.setProgress(rec.matchPercent);

            // UI Logic for "Bridging Program" (Failure case)
            if (rec.program.equals("Bridging Program")) {
                holder.tvMatchPercent.setTextColor(getColor(android.R.color.holo_red_dark));
                holder.tvExplanation.setText("Tap for details");
            } else {
                holder.tvMatchPercent.setTextColor(getColor(R.color.brand_primary));
                holder.tvExplanation.setText("Tap for story");
            }
            holder.tvMatchPercent.setText(rec.matchPercent + "% Match");

            // Click Listener -> Opens Detailed "Wrapped" View
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ResultsActivity.this, WrappedDetailActivity.class);
                intent.putExtra("rec_data", rec);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return recsList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProgram, tvExplanation, tvMatchPercent;
            ProgressBar pbMatch;

            ViewHolder(View v) {
                super(v);
                tvProgram = v.findViewById(R.id.tvProgram);
                tvExplanation = v.findViewById(R.id.tvExplanation);
                tvMatchPercent = v.findViewById(R.id.tvMatchPercent);
                pbMatch = v.findViewById(R.id.pbMatch);
            }
        }
    }
}