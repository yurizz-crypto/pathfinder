package com.prototype.pathfinder.ui;

import com.google.android.material.textfield.TextInputEditText;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;
import java.util.Map;

/**
 * TestInputActivity
 * <p>
 * This is the entry point for the assessment flow.
 * Responsibilities:
 * 1. Accepts a unique Test ID (simulating a physical exam paper ID).
 * 2. Queries the local database to fetch raw aptitude scores (Quant, Verbal, Logical).
 * 3. If valid, passes these scores to the SurveyActivity for the next phase.
 */
public class TestInputActivity extends AppCompatActivity {
    private TextInputEditText etTestId;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_input);

        // Bind Views
        etTestId = findViewById(R.id.etTestId);
        Button btnFetch = findViewById(R.id.btnFetchScores);

        // Initialize Database Connection
        dbManager = new DBManager(this);
        dbManager.open();

        // Handle Fetch Action
        btnFetch.setOnClickListener(v -> {
            String testId = etTestId.getText().toString().trim();

            // Query DB for scores
            Map<String, Integer> scores = dbManager.getScoresById(testId);

            if (!scores.isEmpty()) {
                // Success: Transition to Interest Survey
                Intent intent = new Intent(this, SurveyActivity.class);
                intent.putExtra("test_id", testId);
                // Pass retrieved scores forward
                intent.putExtra("quant", scores.get("quant"));
                intent.putExtra("verbal", scores.get("verbal"));
                intent.putExtra("logical", scores.get("logical"));
                startActivity(intent);
            } else {
                // Failure: Show user feedback (Hint: TEST001 is seeded data)
                Toast.makeText(this, "Invalid Test ID. Try TEST001.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent memory leaks
        dbManager.close();
    }
}