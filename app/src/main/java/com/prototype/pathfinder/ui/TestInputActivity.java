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

public class TestInputActivity extends AppCompatActivity {
    private TextInputEditText etTestId;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_input);

        etTestId = findViewById(R.id.etTestId);
        Button btnFetch = findViewById(R.id.btnFetchScores);
        dbManager = new DBManager(this);
        dbManager.open();

        btnFetch.setOnClickListener(v -> {
            String testId = etTestId.getText().toString().trim();
            Map<String, Integer> scores = dbManager.getScoresById(testId);
            if (!scores.isEmpty()) {
                Intent intent = new Intent(this, SurveyActivity.class);
                intent.putExtra("test_id", testId);
                intent.putExtra("quant", scores.get("quant"));
                intent.putExtra("verbal", scores.get("verbal"));
                intent.putExtra("logical", scores.get("logical"));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Invalid Test ID. Try TEST001.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}