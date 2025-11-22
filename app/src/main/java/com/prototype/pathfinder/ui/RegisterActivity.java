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
 * RegisterActivity
 * <p>
 * Handles new user registration.
 * Validates input and stores credentials in the SQLite database via DBManager.
 */
public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etUsername, etEmail, etPassword;
    private DBManager dbManager;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Init Views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        // Init DB
        dbManager = new DBManager(this);
        dbManager.open();
        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Registration Logic
        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pw = etPassword.getText().toString();

            // 1. Validate Input
            if (username.isEmpty() || email.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Attempt Database Insertion
            if (dbManager.registerUser(username, email, pw)) {
                Toast.makeText(this, "Registered successfully! Please login.", Toast.LENGTH_SHORT).show();

                // Store email temporarily for auto-fill or session context
                prefs.edit().putString("user_email", email).apply();

                // 3. Navigate to Login
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
            } else {
                Toast.makeText(this, "Registration failed. Username or email may already exist.", Toast.LENGTH_SHORT).show();
            }
        });

        // Navigate to Login if account exists
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}