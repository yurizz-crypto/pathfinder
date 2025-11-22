package com.prototype.pathfinder.ui;

import android.content.Intent;
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

public class SurveyActivity extends AppCompatActivity {
    // Raw user responses: Map<QuestionIndex, Score(1-5)>
    private Map<Integer, Integer> userResponses = new HashMap<>();

    // 10 Static Questions
    private final String[] questions = {
            "I enjoy solving complex mathematical problems.",       // 0: Quant
            "I like writing essays or storytelling.",               // 1: Verbal
            "I enjoy logic puzzles and strategy games.",            // 2: Logical
            "I am interested in how computers and software work.",  // 3: Logical/Tech
            "I like speaking in front of groups or debating.",      // 4: Verbal
            "I prefer working with data, statistics, and charts.",  // 5: Quant
            "I enjoy fixing things or understanding how they are built.", // 6: Logical
            "I like drawing, designing, or creating visual art.",   // 7: Creative
            "I enjoy reading books and analyzing literature.",      // 8: Verbal
            "I am curious about scientific theories and experiments." // 9: Logical
    };

    // Map question index to category keys used by RecommendationEngine
    private final String[] categoryMap = {
            "quant_interest",   // Q0
            "verbal_interest",  // Q1
            "logical_interest", // Q2
            "logical_interest", // Q3
            "verbal_interest",  // Q4
            "quant_interest",   // Q5
            "logical_interest", // Q6
            "creative_interest",// Q7
            "verbal_interest",  // Q8
            "logical_interest"  // Q9
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

        // Init views
        llQuestions = findViewById(R.id.llQuestions);
        pbProgress = findViewById(R.id.pbProgress);
        tvProgressText = findViewById(R.id.tvProgressText);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnAnalyze = findViewById(R.id.btnAnalyze);

        dbManager = new DBManager(this);
        dbManager.open();

        // Start with Q1
        showQuestion(0);
        setupButtons();
    }

    private void showQuestion(int index) {
        llQuestions.removeAllViews();  // Clear previous view
        currentQuestion = index;

        // Update progress
        pbProgress.setMax(questions.length);
        pbProgress.setProgress(index + 1);
        tvProgressText.setText(String.format("Question %d of %d", index + 1, questions.length));

        // Create Card for question
        CardView card = new CardView(this);
        card.setRadius(16);
        card.setCardElevation(4);
        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(32, 32, 32, 32);

        // Question Text
        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(questions[index]);
        tvQuestion.setTextSize(20); // Slightly larger for readability
        tvQuestion.setTextColor(getColor(android.R.color.black)); // Ensure contrast on white card
        cardLayout.addView(tvQuestion);

        // RadioGroup for 5-point scale
        RadioGroup rgScale = new RadioGroup(this);
        rgScale.setOrientation(RadioGroup.VERTICAL);
        rgScale.setPadding(0, 24, 0, 0);

        String[] options = {"Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"};
        for (int i = 1; i <= 5; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(options[i - 1]);
            rb.setId(i); // ID 1 to 5
            rb.setTextSize(16);
            rb.setPadding(16, 16, 16, 16);
            rgScale.addView(rb);
        }

        // BUG FIX: Restore previous answer if it exists
        if (userResponses.containsKey(index)) {
            rgScale.check(userResponses.get(index));
        }

        // Save selection immediately when clicked
        rgScale.setOnCheckedChangeListener((group, checkedId) -> {
            userResponses.put(index, checkedId);
        });

        cardLayout.addView(rgScale);
        card.addView(cardLayout);
        llQuestions.addView(card);

        // Fade-in animation
        card.setAlpha(0f);
        card.animate().alpha(1f).setDuration(300).start();

        // Manage Button Visibility
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

    private void processAndFinish() {
        // 1. Aggregate scores by category
        Map<String, Integer> sumScores = new HashMap<>();
        Map<String, Integer> countScores = new HashMap<>();

        for (int i = 0; i < questions.length; i++) {
            String category = categoryMap[i];
            int score = userResponses.getOrDefault(i, 3); // Default neutral if somehow missed

            sumScores.put(category, sumScores.getOrDefault(category, 0) + score);
            countScores.put(category, countScores.getOrDefault(category, 0) + 1);
        }

        // 2. Calculate Averages (Round to nearest integer for Engine compatibility)
        Map<String, Integer> finalScores = new HashMap<>();
        for (String key : sumScores.keySet()) {
            double avg = sumScores.get(key) / (double) countScores.get(key);
            finalScores.put(key, (int) Math.round(avg));
        }

        // 3. Pass to Results
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