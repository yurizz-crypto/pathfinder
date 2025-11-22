package com.prototype.pathfinder.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;
import java.util.HashMap;
import java.util.Map;

/**
 * SurveyActivity
 * <p>
 * Conducts a psychometric interest survey using a 5-point Likert Scale.
 * Logic:
 * 1. Displays questions one by one with a progress bar.
 * 2. Maps specific questions to interest categories (Quant, Verbal, Logical, Creative).
 * 3. Aggregates and averages responses to calculate an "Interest Score" per category.
 * 4. Bundles these results and passes them to the ResultsActivity.
 */
public class SurveyActivity extends AppCompatActivity {
    // Stores user answers: Key = Question Index, Value = Rating (1-5)
    private Map<Integer, Integer> userResponses = new HashMap<>();

    // Survey Questions
    private final String[] questions = {
            "I enjoy solving complex mathematical problems.",
            "I like writing essays or storytelling.",
            "I enjoy logic puzzles and strategy games.",
            "I am interested in how computers and software work.",
            "I like speaking in front of groups or debating.",
            "I prefer working with data, statistics, and charts.",
            "I enjoy fixing things or understanding how they are built.",
            "I like drawing, designing, or creating visual art.",
            "I enjoy reading books and analyzing literature.",
            "I am curious about scientific theories and experiments."
    };

    // Maps Question Index to Interest Category
    private final String[] categoryMap = {
            "quant_interest", "verbal_interest", "logical_interest", "logical_interest",
            "verbal_interest", "quant_interest", "logical_interest", "creative_interest",
            "verbal_interest", "logical_interest"
    };

    private int currentQuestion = 0;
    private LinearLayout llQuestions;
    private ProgressBar pbProgress;
    private TextView tvProgressText;
    private Button btnPrev, btnNext, btnAnalyze;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        // Bind Views
        llQuestions = findViewById(R.id.llQuestions);
        pbProgress = findViewById(R.id.pbProgress);
        tvProgressText = findViewById(R.id.tvProgressText);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnAnalyze = findViewById(R.id.btnAnalyze);

        dbManager = new DBManager(this);
        dbManager.open();

        showQuestion(0);
        setupButtons();
    }

    /**
     * Dynamically generates the UI for the specific question index.
     * Uses a CardView containing the Question Text and a RadioGroup for options.
     *
     * @param index The index of the question to display.
     */
    private void showQuestion(int index) {
        llQuestions.removeAllViews();
        currentQuestion = index;

        // Update Progress
        pbProgress.setMax(questions.length);
        pbProgress.setProgress(index + 1);
        tvProgressText.setText(String.format("Question %d of %d", index + 1, questions.length));

        // Create Card Container
        CardView card = new CardView(this);
        card.setRadius(16);
        card.setCardElevation(4);
        card.setCardBackgroundColor(getColor(R.color.brand_primary)); // Brand Color Background

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(32, 32, 32, 32);

        // Add Question Text
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(questions[index]);
        tvQuestion.setTextSize(20);
        tvQuestion.setTextColor(Color.WHITE);
        cardLayout.addView(tvQuestion);

        // Create Radio Options (Likert Scale)
        RadioGroup rgScale = new RadioGroup(this);
        rgScale.setOrientation(RadioGroup.VERTICAL);
        rgScale.setPadding(0, 24, 0, 0);

        String[] options = {"Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"};
        for (int i = 1; i <= 5; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(options[i - 1]);
            rb.setId(i); // ID matches the score value (1-5)
            rb.setTextSize(16);
            rb.setPadding(16, 16, 16, 16);
            rb.setTextColor(Color.WHITE);
            rb.setButtonTintList(ColorStateList.valueOf(getColor(R.color.brand_secondary)));
            rgScale.addView(rb);
        }

        // Restore previous selection if navigating back
        if (userResponses.containsKey(index)) {
            rgScale.check(userResponses.get(index));
        }

        // Save selection on change
        rgScale.setOnCheckedChangeListener((group, checkedId) -> {
            userResponses.put(index, checkedId);
        });

        cardLayout.addView(rgScale);
        card.addView(cardLayout);
        llQuestions.addView(card);

        // Simple Fade-in Animation
        card.setAlpha(0f);
        card.animate().alpha(1f).setDuration(300).start();

        // Manage Navigation Button Visibility
        btnPrev.setVisibility(index == 0 ? View.GONE : View.VISIBLE);
        if (index == questions.length - 1) {
            btnNext.setVisibility(View.GONE);
            btnAnalyze.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnAnalyze.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        btnPrev.setOnClickListener(v -> {
            if (currentQuestion > 0) showQuestion(currentQuestion - 1);
        });

        btnNext.setOnClickListener(v -> {
            if (!userResponses.containsKey(currentQuestion)) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentQuestion < questions.length - 1) {
                showQuestion(currentQuestion + 1);
            }
        });

        btnAnalyze.setOnClickListener(v -> {
            if (!userResponses.containsKey(currentQuestion)) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }
            processAndFinish();
        });
    }

    /**
     * Aggregates the survey data.
     * Calculates the average score for each category (Quant, Verbal, etc.) based on user responses.
     * Packages the data into a Bundle and starts the ResultsActivity.
     */
    private void processAndFinish() {
        Map<String, Integer> sumScores = new HashMap<>();
        Map<String, Integer> countScores = new HashMap<>();

        // 1. Sum up scores per category
        for (int i = 0; i < questions.length; i++) {
            String category = categoryMap[i];
            int score = userResponses.getOrDefault(i, 3); // Default to Neutral if error
            sumScores.put(category, sumScores.getOrDefault(category, 0) + score);
            countScores.put(category, countScores.getOrDefault(category, 0) + 1);
        }

        // 2. Calculate Averages
        Map<String, Integer> finalScores = new HashMap<>();
        for (String key : sumScores.keySet()) {
            double avg = sumScores.get(key) / (double) countScores.get(key);
            // Result is a 1-5 scale integer for each interest category
            finalScores.put(key, (int) Math.round(avg));
        }

        // 3. Pass Data to Results Engine
        Intent intent = new Intent(this, ResultsActivity.class);
        intent.putExtra("test_id", getIntent().getStringExtra("test_id"));
        Bundle bundle = new Bundle();
        bundle.putSerializable("survey_scores", (java.io.Serializable) finalScores);
        intent.putExtra("bundle", bundle);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}