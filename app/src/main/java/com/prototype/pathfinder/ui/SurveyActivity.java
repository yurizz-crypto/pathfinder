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

public class SurveyActivity extends AppCompatActivity {
    private Map<Integer, Integer> userResponses = new HashMap<>();
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

    private void showQuestion(int index) {
        llQuestions.removeAllViews();
        currentQuestion = index;

        pbProgress.setMax(questions.length);
        pbProgress.setProgress(index + 1);
        tvProgressText.setText(String.format("Question %d of %d", index + 1, questions.length));

        CardView card = new CardView(this);
        card.setRadius(16);
        card.setCardElevation(4);
        // UPDATED: Set Card background to Brand Primary (Green)
        card.setCardBackgroundColor(getColor(R.color.brand_primary));

        LinearLayout cardLayout = new LinearLayout(this);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setPadding(32, 32, 32, 32);

        TextView tvQuestion = new TextView(this);
        tvQuestion.setText(questions[index]);
        tvQuestion.setTextSize(20);
        // UPDATED: Set Question Text to White
        tvQuestion.setTextColor(Color.WHITE);
        cardLayout.addView(tvQuestion);

        RadioGroup rgScale = new RadioGroup(this);
        rgScale.setOrientation(RadioGroup.VERTICAL);
        rgScale.setPadding(0, 24, 0, 0);

        String[] options = {"Strongly Disagree", "Disagree", "Neutral", "Agree", "Strongly Agree"};
        for (int i = 1; i <= 5; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(options[i - 1]);
            rb.setId(i);
            rb.setTextSize(16);
            rb.setPadding(16, 16, 16, 16);
            // UPDATED: Set RadioButton Text to White and Tint to Yellow
            rb.setTextColor(Color.WHITE);
            rb.setButtonTintList(ColorStateList.valueOf(getColor(R.color.brand_secondary)));
            rgScale.addView(rb);
        }

        if (userResponses.containsKey(index)) {
            rgScale.check(userResponses.get(index));
        }

        rgScale.setOnCheckedChangeListener((group, checkedId) -> {
            userResponses.put(index, checkedId);
        });

        cardLayout.addView(rgScale);
        card.addView(cardLayout);
        llQuestions.addView(card);

        card.setAlpha(0f);
        card.animate().alpha(1f).setDuration(300).start();

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
        Map<String, Integer> sumScores = new HashMap<>();
        Map<String, Integer> countScores = new HashMap<>();

        for (int i = 0; i < questions.length; i++) {
            String category = categoryMap[i];
            int score = userResponses.getOrDefault(i, 3);
            sumScores.put(category, sumScores.getOrDefault(category, 0) + score);
            countScores.put(category, countScores.getOrDefault(category, 0) + 1);
        }

        Map<String, Integer> finalScores = new HashMap<>();
        for (String key : sumScores.keySet()) {
            double avg = sumScores.get(key) / (double) countScores.get(key);
            finalScores.put(key, (int) Math.round(avg));
        }

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