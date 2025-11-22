package com.prototype.pathfinder.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;

/**
 * LoginActivity
 * <p>
 * Handles user authentication.
 * Verifies credentials against the SQLite database and initiates the session.
 */
public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private DBManager dbManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Init Views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        // Init DB
        dbManager = new DBManager(this);
        dbManager.open();
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Login Logic
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pw = etPassword.getText().toString();

            // 1. Verify Credentials
            if (dbManager.loginUser(email, pw)) {
                // 2. Save Session (Email)
                prefs.edit().putString("user_email", email).apply();

                // 3. Navigate to Dashboard
                startActivity(new Intent(this, DashboardActivity.class));
                finish(); // Prevent back-navigation to login
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            } else {
                Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to Register
        tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}