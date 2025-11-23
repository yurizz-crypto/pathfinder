package com.prototype.pathfinder.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;
import com.prototype.pathfinder.ui.LoginActivity;
import com.prototype.pathfinder.ui.TestInputActivity;
import com.prototype.pathfinder.ui.WrappedDetailActivity;
import com.prototype.pathfinder.utils.RecommendationEngine;

import java.util.ArrayList;
import java.util.List;

/**
 * Home dashboard fragment shown after login.
 * Displays personalized greeting, saved recommendations,
 * enrollment status card, and quick actions.
 */
public class HomeFragment extends Fragment {

    // === UI Views ===
    private RecyclerView rvRecs;
    private TextView tvRecTitle;
    private TextView tvGreeting;
    private CardView cvStatus;
    private TextView tvStatusMsg;
    private DBManager dbManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        tvGreeting = v.findViewById(R.id.tvGreeting);
        Button btnStart = v.findViewById(R.id.btnStart);
        ImageButton btnLogout = v.findViewById(R.id.btnLogout);
        cvStatus = v.findViewById(R.id.cvStatus);
        tvStatusMsg = v.findViewById(R.id.tvStatusMsg);

        rvRecs = v.findViewById(R.id.rvHomeRecs);
        tvRecTitle = v.findViewById(R.id.tvRecTitle);
        rvRecs.setLayoutManager(new LinearLayoutManager(getContext()));

        // Database & user prefs
        dbManager = new DBManager(getContext());
        dbManager.open();
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);

        // Personalized greeting
        String email = prefs.getString("user_email", "");
        String username = dbManager.getUsername(email);
        tvGreeting.setText("Hello " + username + "!");

        // Start new test
        btnStart.setOnClickListener(view ->
                startActivity(new Intent(getActivity(), TestInputActivity.class)));

        // Logout with confirmation
        btnLogout.setOnClickListener(view -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        prefs.edit().clear().apply();
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedResults(); // Refresh recommendations every time fragment resumes
    }

    /**
     * Loads previously saved recommendations from SharedPreferences.
     * Reconstructs full Recommendation objects including V2 chart data.
     */
    private void loadSavedResults() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_results", Context.MODE_PRIVATE);
        int count = prefs.getInt("rec_count", 0);

        if (count > 0) {
            tvRecTitle.setVisibility(View.VISIBLE);
            List<RecommendationEngine.Recommendation> savedRecs = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String program = prefs.getString("rec_prog_" + i, "");
                int pct = prefs.getInt("rec_pct_" + i, 0);
                String why = prefs.getString("rec_why_" + i, "");
                String hist = prefs.getString("rec_hist_" + i, "");
                String careers = prefs.getString("rec_car_" + i, "");
                String insight = prefs.getString("rec_insight_" + i, "");
                String hardest = prefs.getString("rec_hardest_" + i, "");
                int quant = prefs.getInt("rec_quant_" + i, 70);
                int verbal = prefs.getInt("rec_verbal_" + i, 70);
                int logical = prefs.getInt("rec_logical_" + i, 70);

                // V2: Load chart-specific values with safe defaults
                int target = prefs.getInt("rec_target_" + i, 85);
                int success = prefs.getInt("rec_success_" + i, 92);

                savedRecs.add(new RecommendationEngine.Recommendation(
                        program, pct, why, hist, careers, insight, hardest,
                        quant, verbal, logical,
                        target, success
                ));
            }

            rvRecs.setAdapter(new HomeRecAdapter(savedRecs));

            // === V2.0 Dynamic Status Card ===
            String topProgram = savedRecs.get(0).program;
            if (topProgram.contains("Bridging")) {
                tvStatusMsg.setText("Status: Intervention Recommended");
                cvStatus.setCardBackgroundColor(requireContext().getColor(android.R.color.holo_orange_dark));
            } else {
                tvStatusMsg.setText("Status: Ready to Enroll in " + topProgram);
                cvStatus.setCardBackgroundColor(requireContext().getColor(R.color.brand_primary));
            }

        } else {
            // No results yet
            tvRecTitle.setVisibility(View.GONE);
            rvRecs.setAdapter(null);
            tvStatusMsg.setText("Status: Pending Assessment");
            cvStatus.setCardBackgroundColor(requireContext().getColor(R.color.brand_primary));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }

    /**
     * Adapter for home screen recommendation cards.
     * Slightly different styling than ResultsActivity (e.g. "action plan" text).
     */
    private class HomeRecAdapter extends RecyclerView.Adapter<HomeRecAdapter.ViewHolder> {
        private final List<RecommendationEngine.Recommendation> list;

        public HomeRecAdapter(List<RecommendationEngine.Recommendation> l) {
            this.list = l;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_recommendation, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            RecommendationEngine.Recommendation item = list.get(position);
            holder.tvProgram.setText(item.program);
            holder.tvPct.setText(item.matchPercent + "% Match");
            holder.pb.setProgress(item.matchPercent);

            // Bridging gets red + special CTA
            if (item.program.contains("Bridging")) {
                holder.tvPct.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
                holder.tvExpl.setText("Tap to view action plan");
            } else {
                holder.tvPct.setTextColor(requireContext().getColor(R.color.brand_primary));
                holder.tvExpl.setText("Tap for your full story");
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WrappedDetailActivity.class);
                intent.putExtra("rec_data", item);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvProgram, tvPct, tvExpl;
            android.widget.ProgressBar pb;

            ViewHolder(View v) {
                super(v);
                tvProgram = v.findViewById(R.id.tvProgram);
                tvPct = v.findViewById(R.id.tvMatchPercent);
                tvExpl = v.findViewById(R.id.tvExplanation);
                pb = v.findViewById(R.id.pbMatch);
            }
        }
    }
}