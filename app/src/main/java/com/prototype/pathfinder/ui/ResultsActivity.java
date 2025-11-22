package com.prototype.pathfinder.ui;

import android.content.Intent;
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

public class ResultsActivity extends AppCompatActivity {
    private LottieAnimationView lottieReveal;
    private ProgressBar pbFallback;
    private RecyclerView rvRecs;
    private TextView tvConfidence;
    private TextView tvCalculating;
    private NestedScrollView nestedResults;
    private DBManager dbManager;
    private RecommendationEngine engine;
    private List<RecommendationEngine.Recommendation> recs;  // Class field for share access

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        lottieReveal = findViewById(R.id.lottieReveal);
        pbFallback = findViewById(R.id.pbRevealFallback);
        tvCalculating = findViewById(R.id.tvCalculating);
        rvRecs = findViewById(R.id.rvRecommendations);
        tvConfidence = findViewById(R.id.tvConfidence);
        Button btnShare = findViewById(R.id.btnShare);
        nestedResults = findViewById(R.id.nestedResults);

        dbManager = new DBManager(this);
        dbManager.open();
        engine = new RecommendationEngine(dbManager);

        String testId = getIntent().getStringExtra("test_id");
        Bundle bundle = getIntent().getBundleExtra("bundle");
        @SuppressWarnings("unchecked")
        Map<String, Integer> survey = (Map<String, Integer>) bundle.getSerializable("survey_scores");

        // Try Lottie first; fallback if file missing or class not found
        try {
            int resId = getResources().getIdentifier("sparkles", "raw", getPackageName());
            if (resId != 0) {
                lottieReveal.setAnimation(resId);
                lottieReveal.playAnimation();
                tvCalculating.setVisibility(View.VISIBLE);
            } else {
                throw new Exception("No raw file");
            }
        } catch (Exception e) {
            // Fallback to ProgressBar
            lottieReveal.setVisibility(View.GONE);
            pbFallback.setVisibility(View.VISIBLE);
            tvCalculating.setVisibility(View.GONE);
        }

        // Delay reveal
        new Handler().postDelayed(() -> {
            // Hide reveal elements
            lottieReveal.setVisibility(View.GONE);
            pbFallback.setVisibility(View.GONE);
            tvCalculating.setVisibility(View.GONE);

            // Show results
            nestedResults.setVisibility(View.VISIBLE);

            recs = engine.computeRecommendations(testId, survey);
            rvRecs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rvRecs.setAdapter(new RecAdapter(recs));

            // Confidence: Avg of top 3 (simple loop instead of stream)
            double avg = 0;
            if (recs != null && !recs.isEmpty()) {
                int sum = 0;
                for (RecommendationEngine.Recommendation r : recs) {
                    sum += r.matchPercent;
                }
                avg = sum / (double) recs.size();
            }
            tvConfidence.setText(String.format("Confidence Badge: %.0f%%", avg));
            // Use built-in colors (no R.color.green needed)
            if (avg > 70) {
                tvConfidence.setBackgroundColor(getColor(android.R.color.holo_green_light));
            } else {
                tvConfidence.setBackgroundColor(getColor(android.R.color.holo_orange_light));
            }
        }, 2000);

        btnShare.setOnClickListener(v -> {
            // Mock share: Intent with text summary (safe access to recs)
            String summary = "My CMU Pathfinder Ready!";
            if (recs != null && !recs.isEmpty()) {
                summary = "Top: " + recs.get(0).program + " (" + recs.get(0).matchPercent + "%)";
            }
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, summary);
            startActivity(Intent.createChooser(shareIntent, "Share Wrapped"));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    // RecAdapter for horizontal cards
    private class RecAdapter extends RecyclerView.Adapter<RecAdapter.ViewHolder> {
        private final List<RecommendationEngine.Recommendation> recsList;  // Local copy to avoid conflict

        public RecAdapter(List<RecommendationEngine.Recommendation> r) {
            this.recsList = r;
        }

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
            holder.tvExplanation.setText("Tap to view your Career Story"); // Simplified preview text

            // Click to Open Wrapped Flow
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ResultsActivity.this, WrappedDetailActivity.class);
                intent.putExtra("rec_data", rec); // Pass the object
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_up, R.anim.stay); // Slide up animation
            });
            holder.itemView.animate().alpha(1f).setDuration(500).setStartDelay(position * 300L).start();
        }

        @Override
        public int getItemCount() {
            return recsList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProgram, tvExplanation;
            ProgressBar pbMatch;

            ViewHolder(View v) {
                super(v);
                tvProgram = v.findViewById(R.id.tvProgram);
                tvExplanation = v.findViewById(R.id.tvExplanation);
                pbMatch = v.findViewById(R.id.pbMatch);
            }
        }
    }
}