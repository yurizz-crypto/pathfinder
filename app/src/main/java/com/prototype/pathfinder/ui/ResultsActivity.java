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
import android.widget.Toast;

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
 * Main results screen shown after completing the aptitude test.
 * Features a dramatic 4-second reveal animation, confidence indicator,
 * ranked recommendations list, and sharing functionality.
 */
public class ResultsActivity extends AppCompatActivity {

    // === UI Components ===
    private LottieAnimationView lottieReveal;        // Full-screen reveal animation
    private ProgressBar pbFallback;                  // Fallback spinner if Lottie fails
    private RecyclerView rvRecs;                     // List of top 3 recommendations
    private TextView tvConfidence;                   // Dynamic confidence level + color
    private TextView tvCalculating;                  // "Calculating your path..." text
    private NestedScrollView nestedResults;          // Scrollable results container

    // === Core Logic ===
    private DBManager dbManager;                     // SQLite helper
    private RecommendationEngine engine;             // V2 recommendation logic
    private List<RecommendationEngine.Recommendation> recs; // Final computed results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // Initialize UI
        lottieReveal = findViewById(R.id.lottieReveal);
        pbFallback = findViewById(R.id.pbRevealFallback);
        tvCalculating = findViewById(R.id.tvCalculating);
        rvRecs = findViewById(R.id.rvRecommendations);
        tvConfidence = findViewById(R.id.tvConfidence);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnHome = findViewById(R.id.btnHome);
        nestedResults = findViewById(R.id.nestedResults);

        // Initialize database and engine
        dbManager = new DBManager(this);
        dbManager.open();
        engine = new RecommendationEngine(dbManager);

        // Extract test data from intent
        String testId = getIntent().getStringExtra("test_id");
        Bundle bundle = getIntent().getBundleExtra("bundle");
        Map<String, Integer> survey = (Map<String, Integer>) bundle.getSerializable("survey_scores");

        // === 4-second dramatic reveal ===
        new Handler().postDelayed(() -> {
            // Hide loading UI
            lottieReveal.setVisibility(View.GONE);
            pbFallback.setVisibility(View.GONE);
            tvCalculating.setVisibility(View.GONE);
            nestedResults.setVisibility(View.VISIBLE);

            // Compute recommendations (includes Bridging logic if needed)
            recs = engine.computeRecommendations(testId, survey);
            saveResultsToPrefs(recs);

            // === V2.0 Confidence Indicator ===
            if (!recs.isEmpty()) {
                int topScore = recs.get(0).matchPercent;
                if (topScore >= 90) {
                    tvConfidence.setText("System Confidence: High (98%)");
                    tvConfidence.setTextColor(getColor(R.color.brand_primary));
                } else if (topScore >= 75) {
                    tvConfidence.setText("System Confidence: Moderate (85%)");
                    tvConfidence.setTextColor(getColor(android.R.color.holo_orange_dark));
                } else {
                    tvConfidence.setText("System Confidence: Low - Consider Bridging");
                    tvConfidence.setTextColor(getColor(android.R.color.holo_red_dark));
                }
            }

            // Display results
            rvRecs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rvRecs.setAdapter(new RecAdapter(recs));

        }, 4000); // 4-second delay for drama

        // === Share Button ===
        btnShare.setOnClickListener(v -> {
            if (recs != null && !recs.isEmpty()) {
                RecommendationEngine.Recommendation topResult = recs.get(0);
                String shareBody = "Pathfinder Result:\nTop Match: " + topResult.program + "\nFit: " + topResult.matchPercent + "%";
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "My Pathfinder Result");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share result via"));
            }
        });

        // === Home Button ===
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Persists top 3 recommendations to SharedPreferences for display on HomeFragment.
     * Includes all V2 fields needed for WrappedDetailActivity charts.
     */
    private void saveResultsToPrefs(List<RecommendationEngine.Recommendation> list) {
        SharedPreferences prefs = getSharedPreferences("user_results", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int count = Math.min(list.size(), 3);
        editor.putInt("rec_count", count);

        for (int i = 0; i < count; i++) {
            RecommendationEngine.Recommendation r = list.get(i);
            editor.putString("rec_prog_" + i, r.program);
            editor.putInt("rec_pct_" + i, r.matchPercent);
            editor.putString("rec_why_" + i, r.storyWhy);
            editor.putString("rec_hist_" + i, r.storyHistory);
            editor.putString("rec_car_" + i, r.storyCareers);
            editor.putString("rec_insight_" + i, r.itemInsight);
            editor.putString("rec_hardest_" + i, r.hardestLogical);
            editor.putInt("rec_quant_" + i, r.radarValues[0]);
            editor.putInt("rec_verbal_" + i, r.radarValues[1]);
            editor.putInt("rec_logical_" + i, r.radarValues[2]);
            // V2: Save chart-specific values
            editor.putInt("rec_target_" + i, r.targetScore);
            editor.putInt("rec_success_" + i, r.successRate);
        }
        editor.apply();
    }

    /**
     * RecyclerView adapter for displaying recommendation cards.
     * Highlights Bridging Program with red accent.
     */
    private class RecAdapter extends RecyclerView.Adapter<RecAdapter.ViewHolder> {
        private final List<RecommendationEngine.Recommendation> recsList;

        public RecAdapter(List<RecommendationEngine.Recommendation> r) {
            this.recsList = r;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recommendation, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RecommendationEngine.Recommendation rec = recsList.get(position);
            holder.tvProgram.setText(rec.program);
            holder.pbMatch.setProgress(rec.matchPercent);
            holder.tvMatchPercent.setText(rec.matchPercent + "% Match");

            // Visual distinction for Bridging Program
            if (rec.program.contains("Bridging")) {
                holder.tvMatchPercent.setTextColor(getColor(android.R.color.holo_red_dark));
                holder.tvExplanation.setText("Intervention Required");
            } else {
                holder.tvMatchPercent.setTextColor(getColor(R.color.brand_primary));
                holder.tvExplanation.setText("Tap for story");
            }

            // Open detailed Wrapped view
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ResultsActivity.this, WrappedDetailActivity.class);
                intent.putExtra("rec_data", rec);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return recsList.size();
        }

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