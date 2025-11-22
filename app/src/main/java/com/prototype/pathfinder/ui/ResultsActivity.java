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

public class ResultsActivity extends AppCompatActivity {
    // ... (Previous declarations) ...
    private LottieAnimationView lottieReveal;
    private ProgressBar pbFallback;
    private RecyclerView rvRecs;
    private TextView tvConfidence;
    private TextView tvCalculating;
    private NestedScrollView nestedResults;
    private DBManager dbManager;
    private RecommendationEngine engine;
    private List<RecommendationEngine.Recommendation> recs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        // ... (Previous Init Code) ...
        lottieReveal = findViewById(R.id.lottieReveal);
        pbFallback = findViewById(R.id.pbRevealFallback);
        tvCalculating = findViewById(R.id.tvCalculating);
        rvRecs = findViewById(R.id.rvRecommendations);
        tvConfidence = findViewById(R.id.tvConfidence);
        Button btnShare = findViewById(R.id.btnShare);
        Button btnHome = findViewById(R.id.btnHome); // NEW BUTTON
        nestedResults = findViewById(R.id.nestedResults);

        dbManager = new DBManager(this);
        dbManager.open();
        engine = new RecommendationEngine(dbManager);

        String testId = getIntent().getStringExtra("test_id");
        Bundle bundle = getIntent().getBundleExtra("bundle");
        Map<String, Integer> survey = (Map<String, Integer>) bundle.getSerializable("survey_scores");

        // ... (Animation Code Omitted for Brevity) ...

        new Handler().postDelayed(() -> {
            lottieReveal.setVisibility(View.GONE);
            pbFallback.setVisibility(View.GONE);
            tvCalculating.setVisibility(View.GONE);
            nestedResults.setVisibility(View.VISIBLE);

            recs = engine.computeRecommendations(testId, survey);

            // Save results for Home Fragment
            saveResultsToPrefs(recs);

            // ... (Previous Remedial Logic) ...
            // (Paste the fix for remedial argument order here from previous step)

            rvRecs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            rvRecs.setAdapter(new RecAdapter(recs));

        }, 2000);

        btnShare.setOnClickListener(v -> { /* Share Intent */ });

        // NEW: Back to Home
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    // NEW: Helper to save Top 3 results
    private void saveResultsToPrefs(List<RecommendationEngine.Recommendation> list) {
        SharedPreferences prefs = getSharedPreferences("user_results", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Simple serialization: count|prog1|pct1|why1...
        int count = Math.min(list.size(), 3);
        editor.putInt("rec_count", count);

        for(int i=0; i<count; i++) {
            RecommendationEngine.Recommendation r = list.get(i);
            editor.putString("rec_prog_" + i, r.program);
            editor.putInt("rec_pct_" + i, r.matchPercent);
            editor.putString("rec_why_" + i, r.storyWhy);
            editor.putString("rec_hist_" + i, r.storyHistory);
            editor.putString("rec_car_" + i, r.storyCareers);
        }
        editor.apply();
    }

    // ... (Adapter and ViewHolder logic remains the same) ...
    // Need full file? Let me know. I'm providing the additions.

    // Copy RecAdapter from previous output
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

            // Fix for remedial text color
            if (rec.program.equals("Bridging Program")) {
                holder.tvMatchPercent.setTextColor(getColor(android.R.color.holo_red_dark));
                holder.tvExplanation.setText("Tap for details");
            } else {
                holder.tvMatchPercent.setTextColor(getColor(R.color.brand_primary));
                holder.tvExplanation.setText("Tap for story");
            }
            holder.tvMatchPercent.setText(rec.matchPercent + "% Match");

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