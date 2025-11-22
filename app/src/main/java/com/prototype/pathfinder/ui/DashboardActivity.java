package com.prototype.pathfinder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.prototype.pathfinder.R;

public class DashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        Button btnStart = findViewById(R.id.btnStartPathfinder);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String email = prefs.getString("user_email", "User");
        tvWelcome.setText("Welcome back, " + email + "!");

        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, TestInputActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });
    }
}